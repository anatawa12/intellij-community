// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.workspaceModel.storage

import com.intellij.testFramework.UsefulTestCase.assertOneElement
import com.intellij.workspaceModel.storage.entities.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AbstractEntitiesTest {
  @Test
  fun `simple adding`() {
    val builder = WorkspaceEntityStorageBuilder.create()

    val middleEntity = builder.addMiddleEntity()
    builder.addLeftEntity(sequenceOf(middleEntity))

    val storage = builder.toStorage()

    val leftEntity = assertOneElement(storage.entities(LeftEntity::class.java).toList())
    assertOneElement(leftEntity.children.toList())
  }

  @Test
  fun `modifying left entity`() {
    val builder = WorkspaceEntityStorageBuilder.create()

    val middleEntity = builder.addMiddleEntity("first")
    val leftEntity = builder.addLeftEntity(sequenceOf(middleEntity))

    val anotherMiddleEntity = builder.addMiddleEntity("second")
    builder.modifyEntity(ModifiableLeftEntity::class.java, leftEntity) {
      this.children = sequenceOf(anotherMiddleEntity)
    }

    val storage = builder.toStorage()

    val actualLeftEntity = assertOneElement(storage.entities(LeftEntity::class.java).toList())
    val actualChild = assertOneElement(actualLeftEntity.children.toList())
    assertEquals(anotherMiddleEntity, actualChild)
    assertEquals(anotherMiddleEntity.property, (actualChild as MiddleEntity).property)
  }

  @Test
  fun `modifying abstract entity`() {
    val builder = WorkspaceEntityStorageBuilder.create()

    val middleEntity = builder.addMiddleEntity()
    val leftEntity: CompositeBaseEntity = builder.addLeftEntity(sequenceOf(middleEntity))

    val anotherMiddleEntity = builder.addMiddleEntity()
    builder.modifyEntity(ModifiableCompositeBaseEntity::class.java, leftEntity) {
      this.children = sequenceOf(anotherMiddleEntity)
    }

    val storage = builder.toStorage()

    val actualLeftEntity = assertOneElement(storage.entities(LeftEntity::class.java).toList())
    val actualChild = assertOneElement(actualLeftEntity.children.toList())
    assertEquals(anotherMiddleEntity, actualChild)
    assertEquals(anotherMiddleEntity.property, (actualChild as MiddleEntity).property)
  }

  @Test
  fun `children replace in addDiff`() {
    val builder = WorkspaceEntityStorageBuilder.create()
    val middleEntity = builder.addMiddleEntity()
    val leftEntity: CompositeBaseEntity = builder.addLeftEntity(sequenceOf(middleEntity))

    val anotherBuilder = WorkspaceEntityStorageBuilder.from(builder)
    val anotherMiddleEntity = anotherBuilder.addMiddleEntity("Another")
    anotherBuilder.modifyEntity(ModifiableLeftEntity::class.java, leftEntity) {
      this.children = sequenceOf(middleEntity, anotherMiddleEntity)
    }

    val initialMiddleEntity = builder.addMiddleEntity("Initial")
    builder.modifyEntity(ModifiableLeftEntity::class.java, leftEntity) {
      this.children = sequenceOf(middleEntity, initialMiddleEntity)
    }

    builder.addDiff(anotherBuilder)

    val actualLeftEntity = assertOneElement(builder.entities(LeftEntity::class.java).toList())
    val children = actualLeftEntity.children.toList() as List<MiddleEntity>
    assertEquals(2, children.size)
    assertTrue(children.any { it.property == "Another" })
    assertTrue(children.none { it.property == "Initial" })
  }
}
