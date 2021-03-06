// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInspection.dataFlow.lang.ir;

import com.intellij.codeInspection.dataFlow.ReturnTransfer;
import com.intellij.codeInspection.dataFlow.lang.ir.inst.Instruction;
import com.intellij.codeInspection.dataFlow.lang.ir.inst.PushInstruction;
import com.intellij.codeInspection.dataFlow.lang.ir.inst.ReturnInstruction;
import com.intellij.codeInspection.dataFlow.lang.ir.inst.SpliceInstruction;
import com.intellij.codeInspection.dataFlow.value.DfaValue;
import com.intellij.codeInspection.dataFlow.value.DfaValueFactory;
import com.intellij.codeInspection.dataFlow.value.DfaVariableValue;
import com.intellij.codeInspection.dataFlow.value.VariableDescriptor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import com.intellij.util.containers.FList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import one.util.streamex.StreamEx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Represents code block IR (list of instructions)
 */
public final class ControlFlow {
  private @NotNull final List<Instruction> myInstructions;
  private @NotNull final Object2IntMap<PsiElement> myElementToStartOffsetMap;
  private @NotNull final Object2IntMap<PsiElement> myElementToEndOffsetMap;
  private @NotNull final DfaValueFactory myFactory;
  private int[] myLoopNumbers;

  public ControlFlow(@NotNull final DfaValueFactory factory) {
    myFactory = factory;
    myInstructions = new ArrayList<>();
    myElementToEndOffsetMap = new Object2IntOpenHashMap<>();
    myElementToStartOffsetMap = new Object2IntOpenHashMap<>();
  }

  /**
   * Copy constructor to bind existing flow to another factory. The newly-created flow should not depend on the original factory
   * but may reuse as much of data as possible. The modifications in new flow are prohibited
   *
   * @param flow    flow to copy
   * @param factory factory to use
   */
  public ControlFlow(@NotNull ControlFlow flow, @NotNull DfaValueFactory factory) {
    myFactory = factory;
    myElementToEndOffsetMap = flow.myElementToEndOffsetMap;
    myElementToStartOffsetMap = flow.myElementToStartOffsetMap;
    myLoopNumbers = flow.myLoopNumbers;
    myInstructions = StreamEx.of(flow.myInstructions).map(instruction -> instruction.bindToFactory(factory)).toImmutableList();
  }

  public Instruction[] getInstructions(){
    return myInstructions.toArray(new Instruction[0]);
  }

  public Instruction getInstruction(int index) {
    return myInstructions.get(index);
  }

  public int getInstructionCount() {
    return myInstructions.size();
  }

  public ControlFlowOffset getNextOffset() {
    return new FixedOffset(myInstructions.size());
  }

  public void startElement(PsiElement psiElement) {
    myElementToStartOffsetMap.put(psiElement, myInstructions.size());
  }

  public void finishElement(PsiElement psiElement) {
    myElementToEndOffsetMap.put(psiElement, myInstructions.size());
  }

  public void addInstruction(Instruction instruction) {
    instruction.setIndex(myInstructions.size());
    myInstructions.add(instruction);
  }

  public int[] getLoopNumbers() {
    return myLoopNumbers;
  }

  /**
   * Finalize current control flow. No more instructions are accepted after this call
   */
  public void finish() {
    addInstruction(new ReturnInstruction(myFactory.controlTransfer(ReturnTransfer.INSTANCE, FList.emptyList()), null));

    myLoopNumbers = LoopAnalyzer.calcInLoop(this);
    new LiveVariablesAnalyzer(this).flushDeadVariablesOnStatementFinish();
  }

  /**
   * @return stream of all accessed variables within this flow
   */
  public Stream<DfaVariableValue> accessedVariables() {
    return StreamEx.of(myInstructions).select(PushInstruction.class)
      .remove(PushInstruction::isReferenceWrite)
      .map(PushInstruction::getValue)
      .select(DfaVariableValue.class).distinct();
  }

  public ControlFlowOffset getStartOffset(final PsiElement element) {
    return new FromMapOffset(element, myElementToStartOffsetMap);
  }

  public ControlFlowOffset getEndOffset(final PsiElement element) {
    return new FromMapOffset(element, myElementToEndOffsetMap);
  }

  public String toString() {
    StringBuilder result = new StringBuilder();
    final List<Instruction> instructions = myInstructions;

    for (int i = 0; i < instructions.size(); i++) {
      Instruction instruction = instructions.get(i);
      result.append(i).append(": ").append(instruction.toString());
      result.append("\n");
    }
    return result.toString();
  }

  /**
   * Replaces instruction at a given index with NOP
   * @param index instruction index to replace
   */
  public void makeNop(int index) {
    SpliceInstruction instruction = new SpliceInstruction(0);
    instruction.setIndex(index);
    myInstructions.set(index, instruction);
  }

  public @NotNull DfaValueFactory getFactory() {
    return myFactory;
  }

  /**
   * Checks whether supplied variable is a temporary variable created previously via {@link #createTempVariable(PsiType)}
   *
   * @param variable to check
   * @return true if supplied variable is a temp variable.
   */
  public static boolean isTempVariable(@NotNull DfaVariableValue variable) {
    return variable.getDescriptor() instanceof Synthetic;
  }

  /**
   * Create a synthetic variable (not declared in the original code) to be used within this control flow.
   *
   * @param type a type of variable to create
   * @return newly created variable
   */
  @NotNull
  public DfaVariableValue createTempVariable(@Nullable PsiType type) {
    if(type == null) {
      type = PsiType.VOID;
    }
    return getFactory().getVarFactory().createVariableValue(new ControlFlow.Synthetic(getInstructionCount(), type));
  }

  public @NotNull List<DfaVariableValue> getSynthetics(PsiElement element) {
    int startOffset = getStartOffset(element).getInstructionOffset();
    List<DfaVariableValue> synthetics = new ArrayList<>();
    for (DfaValue value : myFactory.getValues()) {
      if (value instanceof DfaVariableValue) {
        DfaVariableValue var = (DfaVariableValue)value;
        VariableDescriptor descriptor = var.getDescriptor();
        if (descriptor instanceof Synthetic && ((Synthetic)descriptor).myLocation >= startOffset) {
          synthetics.add(var);
        }
      }
    }
    return synthetics;
  }

  public abstract static class ControlFlowOffset {
    public abstract int getInstructionOffset();

    @Override
    public String toString() {
      return String.valueOf(getInstructionOffset());
    }
  }

  public static class FixedOffset extends ControlFlowOffset {
    private final int myOffset;

    public FixedOffset(int offset) {
      myOffset = offset;
    }

    @Override
    public int getInstructionOffset() {
      return myOffset;
    }
  }

  public static class DeferredOffset extends ControlFlowOffset {
    private int myOffset = -1;

    @Override
    public int getInstructionOffset() {
      if (myOffset == -1) {
        throw new IllegalStateException("Not set");
      }
      return myOffset;
    }

    public void setOffset(int offset) {
      if (myOffset != -1) {
        throw new IllegalStateException("Already set");
      }
      else {
        myOffset = offset;
      }
    }

    @Override
    public String toString() {
      return myOffset == -1 ? "<not set>" : super.toString();
    }
  }

  private static class FromMapOffset extends ControlFlowOffset {
    private final PsiElement myElement;
    private final Object2IntMap<PsiElement> myElementMap;

    private FromMapOffset(PsiElement element, Object2IntMap<PsiElement> map) {
      myElement = element;
      myElementMap = map;
    }

    @Override
    public int getInstructionOffset() {
      return myElementMap.getInt(myElement);
    }
  }

  public static final class Synthetic implements VariableDescriptor {
    private final int myLocation;
    private final PsiType myType;

    private Synthetic(int location, PsiType type) {
      myLocation = location;
      myType = type;
    }

    @Override
    public @NotNull String toString() {
      return "tmp$" + myLocation;
    }

    @Override
    public @Nullable PsiType getType(@Nullable DfaVariableValue qualifier) {
      return myType;
    }

    @Override
    public boolean isStable() {
      return true;
    }
  }
}