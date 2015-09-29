/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.ui.popup.list;

import com.intellij.openapi.ui.popup.ListItemDescriptor;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.ErrorLabel;
import com.intellij.ui.GroupedElementsRenderer;
import com.intellij.ui.components.panels.OpaquePanel;

import javax.swing.*;
import java.awt.*;

public class GroupedItemsListRenderer extends GroupedElementsRenderer.List implements ListCellRenderer {
  protected ListItemDescriptor myDescriptor;

  protected JLabel myNextStepLabel;

  public JLabel getNextStepLabel() {
    return myNextStepLabel;
  }


  public GroupedItemsListRenderer(ListItemDescriptor descriptor) {
    myDescriptor = descriptor;
  }

  @Override
  public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
    String caption = myDescriptor.getCaptionAboveOf(value);
    boolean hasSeparator = myDescriptor.hasSeparatorAboveOf(value);
    if (index == 0 && StringUtil.isEmptyOrSpaces(caption)) hasSeparator = false;

    Icon icon = myDescriptor.getIconFor(value);
    final JComponent result = configureComponent(myDescriptor.getTextFor(value), myDescriptor.getTooltipFor(value),
                                                 icon, icon, isSelected, hasSeparator,
                                                 caption, -1);
    customizeComponent(list, value, isSelected);
    return result;
  }


  @Override
  protected JComponent createItemComponent() {
    createLabel();
    return layoutComponent(myTextLabel);
  }

  protected void createLabel() {
    myTextLabel = new ErrorLabel();
    myTextLabel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
    myTextLabel.setOpaque(true);
  }

  protected final JComponent layoutComponent(JComponent middleItemComponent) {
    JPanel result = new OpaquePanel(Registry.is("ide.new.project.settings") ? new BorderLayout(0, 0) : new BorderLayout(4, 4), Color.white);

    myNextStepLabel = new JLabel();
    myNextStepLabel.setOpaque(true);

    result.add(middleItemComponent, BorderLayout.CENTER);
    result.add(myNextStepLabel, BorderLayout.EAST);

    result.setBorder(getDefaultItemComponentBorder());

    return result;
  }

  protected void customizeComponent(JList list, Object value, boolean isSelected) {
  }
}
