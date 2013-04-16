package models

import org.joda.time.DateTime

case class Person(
//   id: Option[Int]
   first_name: String
  ,last_name: String
  ,company: Option[String]
  ,host: String
  ,phone: Option[String]
  ,email: Option[String]
  ,nda_accepted: Boolean
  ,created_at: DateTime = DateTime.now()
)

trait PersonStorageComponent {
  this: Storage =>

  import java.sql.Timestamp
  import org.joda.time.DateTime
  import org.joda.time.DateTimeZone.UTC
  import scala.slick.lifted.MappedTypeMapper.base
  import scala.slick.lifted.TypeMapper

  implicit val DateTimeMapper: TypeMapper[DateTime] =
    base[DateTime, Timestamp](
      d => new Timestamp(d.getMillis),
      t => new DateTime(t.getTime, UTC))


  import profile.simple._

  object Persons extends Table[Person]("persons") {
//    def id = column[Int]("id", O.PrimaryKey)
    def first_name =  column[String]("first_name", O.NotNull)
    def last_name =  column[String]("last_name", O.NotNull)
    def company =  column[Option[String]]("company")
    def host =  column[String]("host")
    def phone =  column[Option[String]]("phone")
    def email =  column[Option[String]]("email")
    def nda_accepted =  column[Boolean]("nda_accepted")
    def created_at = column[DateTime]("created_at")

    def * = (first_name ~ last_name ~ company ~ host ~ phone ~ email ~ nda_accepted ~ created_at) <> (Person, Person.unapply _)

    def get()(implicit session: Session): Seq[Person] = {
      val persons = for {
        p <- Persons
      } yield (p)

      persons.list
    }

    def signout(first_name: String, last_name: String)(implicit session: Session) = {
      val person = for {
        p <- Persons if p.first_name === first_name && p.last_name === last_name
      } yield (p.last_name)

      person.foreach(p => {
        Persons.where(_.last_name === p).delete
      })
    }

//    def get(name: String)(implicit session: Session): Option[User] = {
//      val a = for {
//        c <- Users if c.name === name
//      } yield (c)
//
//      a.foreach { x:User =>
//        return Some(x)
//      }
//
//      return None
//    }
//
    def add(p: Person)(implicit session: Session) = {
      this.insert(p)
    }
//
//    def countByName(name: String)(implicit session: Session) = {
//      (for {
//        user <- Users
//        if (user.name === name)
//      } yield(user)).list.size
//    }

  }
}