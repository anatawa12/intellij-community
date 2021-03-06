/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.codeInspection.dataFlow.lang.ir.inst;

import com.intellij.codeInspection.dataFlow.*;
import com.intellij.codeInspection.dataFlow.value.DfaValueFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CheckNotNullInstruction extends Instruction {
  private final @NotNull NullabilityProblemKind.NullabilityProblem<?> myProblem;
  private final @Nullable DfaControlTransferValue myTransferValue;

  public CheckNotNullInstruction(@NotNull NullabilityProblemKind.NullabilityProblem<?> problem,
                                 @Nullable DfaControlTransferValue transferValue) {
    myProblem = problem;
    myTransferValue = transferValue;
  }

  @Override
  public @NotNull Instruction bindToFactory(@NotNull DfaValueFactory factory) {
    if (myTransferValue == null) return this;
    var instruction = new CheckNotNullInstruction(myProblem, myTransferValue.bindToFactory(factory));
    instruction.setIndex(getIndex());
    return instruction;
  }

  @Nullable
  public DfaControlTransferValue getOnNullTransfer() {
    return myTransferValue;
  }

  @NotNull
  public NullabilityProblemKind.NullabilityProblem<?> getProblem() {
    return myProblem;
  }

  @Override
  public DfaInstructionState[] accept(DataFlowRunner runner, DfaMemoryState stateBefore, InstructionVisitor visitor) {
    return visitor.visitCheckNotNull(this, runner, stateBefore);
  }

  @Override
  public String toString() {
    return "CHECK_NOT_NULL "+myProblem;
  }
}
