/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.jetbrains.python;

import com.intellij.spellchecker.inspections.SpellCheckingInspection;
import com.jetbrains.python.fixtures.PyTestCase;
import com.jetbrains.python.psi.LanguageLevel;

/**
 * @author yole
 */
public class PySpellCheckerTest extends PyTestCase {
  public void testPlainTextSplitter() {
    doTest();
  }

  public void testPlainTextSplitter2() {
    doTest();
  }

  public void testPlainTextSplitter3() {
    doTest();
  }

  public void testTypoAfterEscapeSequence() {  // PY-4440
    doTest();
  }

  public void testIgnoreEscapeSequence() {  // PY-6794
    runWithLanguageLevel(LanguageLevel.PYTHON27, this::doTest);
  }

  // PY-20824
  public void testFStringPrefix() {
    doTest();
  }

  public void testFStringExpression() {
    doTest();
  }

  public void testRawFString() {
    doTest();
  }

  // PY-34873
  public void testFStringsWithApostrophe() {
    doTest();
  }

  // PY-20987
  public void testEscapesInRawAndNormalGluedStringElements() {
    doTest();
  }

  // PY-20987
  public void testGluedStringNodesAfterFirstWithPrefix() {
    doTest();
  }

  public void testTyposInInjectedPythonStringsReportedOnce() {
    doTest();
  }

  // PY-36912
  public void testTyposInDoctestsReportedOnce() {
    doTest();
  }

  // PY-7711
  public void testTyposInRegexIgnored() {
    doTest();
  }

  private void doTest() {
    myFixture.enableInspections(SpellCheckingInspection.class);
    myFixture.configureByFile("inspections/spelling/" + getTestName(true) + ".py");
    myFixture.checkHighlighting(true, false, true);
  }
}
