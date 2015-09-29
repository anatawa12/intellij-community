/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.openapi.options.newEditor;

import com.intellij.CommonBundle;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.help.HelpManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurableGroup;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.options.ex.ConfigurableVisitor;
import com.intellij.openapi.project.DumbModePermission;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.Gray;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.JBColor;
import com.intellij.ui.border.CustomLineBorder;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.Map;

/**
 * @author Konstantin Bulenkov
 */
public class IdeSettingsDialog extends DialogWrapper implements DataProvider {
  private Project myProject;
  private ConfigurableGroup[] myGroups;
  private Configurable myPreselected;
  private OptionsEditor myEditor;

  private ApplyAction myApplyAction;
  public static final String DIMENSION_KEY = "OptionsEditor";
  @NonNls static final String LAST_SELECTED_CONFIGURABLE = "options.lastSelected";

  /**
   * This constructor should be eliminated after the new modality approach
   * will have been checked. See a {@code Registry} key ide.perProjectModality
   *
   * @deprecated
   */
  public IdeSettingsDialog(Project project, ConfigurableGroup[] groups,
                           @Nullable Configurable preselectedConfigurable, boolean applicationModalIfPossible) {
    super(project, true, applicationModalIfPossible);
    init(project, groups, preselectedConfigurable != null ? preselectedConfigurable : findLastSavedConfigurable(groups, project));
  }

  /**
   * This constructor should be eliminated after the new modality approach
   * will have been checked. See a {@code Registry} key ide.perProjectModality
   *
   * @deprecated
   */
  public IdeSettingsDialog(Project project, ConfigurableGroup[] groups,
                           @NotNull String preselectedConfigurableDisplayName, boolean applicationModalIfPossible) {
    super(project, true, applicationModalIfPossible);
    init(project, groups, getPreselectedByDisplayName(groups, preselectedConfigurableDisplayName, project));
  }

  public IdeSettingsDialog(Project project, ConfigurableGroup[] groups, @Nullable Configurable preselectedConfigurable) {
    super(project, true);
    init(project, groups, preselectedConfigurable != null ? preselectedConfigurable : findLastSavedConfigurable(groups, project));
  }

  public IdeSettingsDialog(Project project, ConfigurableGroup[] groups, @NotNull String preselectedConfigurableDisplayName) {
    super(project, true);
    init(project, groups, getPreselectedByDisplayName(groups, preselectedConfigurableDisplayName, project));
  }

  @Nullable
  @Override
  protected Border createContentPaneBorder() {
    return IdeBorderFactory.createEmptyBorder(0);
  }

  private void init(final Project project, final ConfigurableGroup[] groups, @Nullable final Configurable preselected) {
    myProject = project;
    myGroups = groups;
    myPreselected = preselected;

    setTitle(CommonBundle.settingsTitle());

    init();
  }

  @Nullable
  private static Configurable getPreselectedByDisplayName(final ConfigurableGroup[] groups, final String preselectedConfigurableDisplayName,
                                                          final Project project) {
    Configurable result = new ConfigurableVisitor.ByName(preselectedConfigurableDisplayName).find(groups);

    return result == null ? findLastSavedConfigurable(groups, project) : result;
  }

  @Override
  public boolean isTypeAheadEnabled() {
    return true;
  }

  @Nullable
  @Override
  protected JComponent createSouthPanel() {
    final JComponent panel = super.createSouthPanel();
    CustomLineBorder line = new CustomLineBorder(new JBColor(Gray._153.withAlpha(128), Gray._100.withAlpha(128)), 1, 0, 0, 0);
    panel.setBorder(new CompoundBorder(line, new EmptyBorder(8, 12, 8, 12)));
    return panel;
  }

  protected JComponent createCenterPanel() {
    myEditor = new OptionsEditor(myProject, myGroups, myPreselected);
    myEditor.getContext().addColleague(new OptionsEditorColleague.Adapter() {
      @Override
      public ActionCallback onModifiedAdded(final Configurable configurable) {
        updateStatus();
        return ActionCallback.DONE;
      }

      @Override
      public ActionCallback onModifiedRemoved(final Configurable configurable) {
        updateStatus();
        return ActionCallback.DONE;
      }

      @Override
      public ActionCallback onErrorsChanged() {
        updateStatus();
        return ActionCallback.DONE;
      }
    });
    Disposer.register(myDisposable, myEditor);
    return myEditor;
  }

  public boolean updateStatus() {
    myApplyAction.setEnabled(myEditor.canApply());

    final Map<Configurable, ConfigurationException> errors = myEditor.getContext().getErrors();
    if (errors.size() == 0) {
      setErrorText(null);
    }
    else {
      String text = "Changes were not applied because of an error";

      final String errorMessage = getErrorMessage(errors);
      if (errorMessage != null) {
        text += "<br>" + errorMessage;
      }

      setErrorText(text);
    }

    return errors.size() == 0;
  }

  @Nullable
  private static String getErrorMessage(final Map<Configurable, ConfigurationException> errors) {
    final Collection<ConfigurationException> values = errors.values();
    final ConfigurationException[] exceptions = values.toArray(new ConfigurationException[values.size()]);
    if (exceptions.length > 0) {
      return exceptions[0].getMessage();
    }
    return null;
  }

  @Override
  protected String getDimensionServiceKey() {
    return DIMENSION_KEY;
  }

  @Override
  public void doOKAction() {
    myEditor.flushModifications();

    DumbService.allowStartingDumbModeInside(DumbModePermission.MAY_START_BACKGROUND, new Runnable() {
      public void run() {
        if (myEditor.canApply()) {
          myEditor.apply();
          if (!updateStatus()) return;
        }

        saveCurrentConfigurable();

        ApplicationManager.getApplication().saveAll();

        IdeSettingsDialog.super.doOKAction();
      }
    });
  }


  private void saveCurrentConfigurable() {
    final Configurable current = myEditor.getContext().getCurrentConfigurable();
    if (current == null) return;

    final PropertiesComponent props = PropertiesComponent.getInstance(myProject);

    if (current instanceof SearchableConfigurable) {
      props.setValue(LAST_SELECTED_CONFIGURABLE, ((SearchableConfigurable)current).getId());
    }
    else {
      props.setValue(LAST_SELECTED_CONFIGURABLE, current.getClass().getName());
    }
  }

  @Nullable
  private static Configurable findLastSavedConfigurable(ConfigurableGroup[] groups, final Project project) {
    final String id = PropertiesComponent.getInstance(project).getValue(LAST_SELECTED_CONFIGURABLE);
    if (id == null) return null;

    return new ConfigurableVisitor.ByID(id).find(groups);
  }

  @Override
  public void doCancelAction(final AWTEvent source) {
    if (source instanceof KeyEvent || source instanceof ActionEvent) {
      if (myEditor.getContext().isHoldingFilter()) {
        myEditor.clearFilter();
        return;
      }
    }

    super.doCancelAction(source);
  }

  @Override
  public void doCancelAction() {
    saveCurrentConfigurable();
    super.doCancelAction();
  }

  @NotNull
  @Override
  protected Action[] createActions() {
    myApplyAction = new ApplyAction();
    return new Action[]{getOKAction(), getCancelAction(), myApplyAction, getHelpAction()};
  }

  @Override
  protected void doHelpAction() {
    final String topic = myEditor.getHelpTopic();
    if (topic != null) {
      HelpManager.getInstance().invokeHelp(topic);
    }
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myEditor.getPreferredFocusedComponent();
  }

  public Object getData(@NonNls String dataId) {
    if (OptionsEditor.KEY.is(dataId)) {
      return myEditor;
    }
    return myEditor == null ? null : myEditor.getData(dataId);
  }

  private class ApplyAction extends AbstractAction {
    public ApplyAction() {
      super(CommonBundle.getApplyButtonText());
      setEnabled(false);
    }

    public void actionPerformed(final ActionEvent e) {
      DumbService.allowStartingDumbModeInside(DumbModePermission.MAY_START_BACKGROUND, new Runnable() {
        @Override
        public void run() {
          myEditor.apply();
        }
      });
      myEditor.revalidate();
      myEditor.repaint();
      updateStatus();
    }
  }
}
