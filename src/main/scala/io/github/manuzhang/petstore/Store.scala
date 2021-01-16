package io.github.manuzhang.petstore

import io.getquill.{H2JdbcContext, SnakeCase}
import io.github.manuzhang.petstore.model._
import io.github.manuzhang.petstore.model.Pet._
import io.github.manuzhang.petstore.model.Pet.Status.Status

object Store {
  
  lazy val instance = new Store
}

class Store {
  private val ctx = new H2JdbcContext(SnakeCase, "petstore")
  
  import ctx._

  def getOrders: List[Order] = {
    ctx.run(query[OrderTable]).map(_.toOrder)
  }
  
  def getOrder(orderId: Long): Option[Order] = {
    ctx.run(query[OrderTable].filter(_.id == lift(orderId))).headOption.map(_.toOrder)
  }
  
  def deleteOrder(orderId: Long): Option[Order] = {
    ctx.transaction {
      getOrder(orderId).map { order =>
        ctx.run(query[OrderTable].filter(_.id == lift(orderId)).delete)
        order
      }
    }
  }
  
  def addOrder(order: Order): Unit = {
    ctx.run(query[OrderTable].insert(lift(order.toTable)))
  }
  
  def getPet(petId: Long): Option[Pet] = {
    toPet(ctx.run(query[PetTable].filter(_.id == lift(petId))))
  }
  
  def getPet(status: Status): List[Pet] = {
    toPets(ctx.run(query[PetTable].filter(_.status == lift(status.toString))))
  }

  def getPet(tags: List[String]): List[Pet] = {
    toPets(ctx.run(query[PetTable].filter(p => liftQuery(tags).contains(p.tagName))))
  }

  def addPet(pet: Pet): Unit = {
    ctx.run(liftQuery(pet.petTable).foreach(p => query[PetTable].insert(p)))
  }
  
  def updatePet(pet: Pet): Option[Pet] = {
    ctx.transaction {
      getPet(pet.id).headOption.map { _ =>
        liftQuery(pet.petTable).foreach(p => query[PetTable].update(p))
        pet
      }
    }
  }
  
  def updatePet(id: Long, name: Option[String], status: Option[String]): Option[Pet] = {
    ctx.transaction {
      getPet(id).headOption.map { pet =>
        var p = pet
        name.foreach(n => p = p.copy(name = n))
        status.foreach(s => p = p.copy(status = Status.withName(s)))
        liftQuery(pet.petTable).foreach(p => query[PetTable].update(p))
        pet
      }
    }
  }
  
  def deletePet(petId: Long): Option[Pet] = {
    ctx.transaction {
      getPet(petId).headOption.map { pet =>
        ctx.run(query[PetTable].filter(_.id == lift(petId)).delete)
        pet
      }
    }
  }
  
  def addUser(user: User): Unit = {
    ctx.run(query[User].insert(lift(user)))
  }
  
  def getUser(username: String): Option[User] = {
    ctx.run(query[User].filter(u => u.username == lift(username)))
  }
  
  def updateUser(username: String, user: User): Option[User] = {
    ctx.transaction {
      getUser(username).headOption.map { prev =>
        ctx.run(query[User].update(lift(user)))
        prev
      }
    }
  }
  
  def deleteUser(username: String): Option[User] = {
    ctx.transaction {
      getUser(username).headOption.map { user =>
        ctx.run(query[User].filter(_.username == lift(username)).delete)
        user
      }
    }
  }
}
