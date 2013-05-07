package models

import java.sql.Timestamp
import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC
import scala.slick.lifted.MappedTypeMapper.base
import scala.slick.lifted.{MappedTypeMapper, TypeMapper, BaseTypeMapper}


import org.joda.time.DateTime
import java.util.UUID
import scala.slick.lifted.MappedTypeMapper._

object VisitType extends Enumeration {
  type VisitType = Value
  val Meeting, Event, Interview = Value

  def fromString(s: String): VisitType.VisitType = {
    VisitType.values.find(_.toString == s).get
  }

  implicit def visitTypeMapper[VisitType] = base[VisitType.VisitType, String] (
    { _.toString },
    { VisitType.fromString(_) })
}

case class Person(
   id: UUID
  ,first_name: String
  ,last_name: String
  ,company: Option[String]
  ,host: String
  ,phone: Option[String]
  ,email: Option[String]
  ,visit_type: VisitType.VisitType
  ,nda_accepted: Boolean
  ,created_at: Timestamp
  ,signed_out: Boolean = false
)


trait PersonStorageComponent {
  this: Storage =>


  implicit val DateTimeMapper = new MappedTypeMapper[DateTime, java.sql.Timestamp] with BaseTypeMapper[DateTime] {
    def map(t: DateTime): java.sql.Timestamp = new Timestamp(t.getMillis)
    def comap(t: java.sql.Timestamp): DateTime = new DateTime(t.getTime, UTC)
    override def sqlTypeName = Some("DATETIME")
  }

//  implicit val VisitTypeTypeMapper1: BaseTypeMapper[VisitType] =  base[VisitType, String]({_.toString}, VisitType.fromString)
//  implicit val VisitTypeTypeMapper2: BaseTypeMapper[VisitType] = base[VisitType, String]({_.toString}, VisitType.fromString)
//  implicit val VisitTypeTypeMapper3: BaseTypeMapper[VisitType] = base[VisitType, String]({_.toString}, VisitType.fromString)
//  implicit val VisitTypeTypeMapper4: BaseTypeMapper[VisitType] = base[VisitType, String]({_.toString}, VisitType.fromString)
//
//  implicit val VisitTypeTypeMapper: BaseTypeMapper[U <: VisitType] = base[U, String]({_.toString}, VisitType.fromString)

  implicit def timestamp2datetime(a: Timestamp): DateTime = new DateTime(a.getTime, UTC)
  implicit def datetime2timestamp(a: DateTime): Timestamp = new Timestamp(a.getMillis)

  import profile.simple._

  object Persons extends Table[Person]("persons") {
    def id = column[UUID]("id", O.PrimaryKey)
    def first_name =  column[String]("first_name", O.NotNull)
    def last_name =  column[String]("last_name", O.NotNull)
    def company =  column[Option[String]]("company")
    def host =  column[String]("host")
    def phone =  column[Option[String]]("phone")
    def email =  column[Option[String]]("email")
    def visit_type =  column[VisitType.VisitType]("visit_type")
    def nda_accepted =  column[Boolean]("nda_accepted")
    def created_at = column[Timestamp]("created_at")
    def signed_out =  column[Boolean]("signed_out")

    def * = (id ~ first_name ~ last_name ~ company ~ host ~ phone ~ email ~ visit_type ~ nda_accepted ~ created_at ~ signed_out) <> (Person, Person.unapply _)

    def get()(implicit session: Session): Seq[Person] = {
      val persons = for {
        p <- Persons
      } yield (p)

      persons.list
    }

    def get(needle: UUID)(implicit session: Session): Option[Person] = {
      val persons = for {
        p <- Persons if p.id === needle.bind
      } yield (p)

      persons.list.headOption
    }

    def present()(implicit session: Session): Seq[Person] = {
      Query(Persons)
        .filter(_.signed_out === false)
        .filter(_.created_at > datetime2timestamp(DateTime.now().minusHours(12)))
        .sortBy(_.created_at.desc)
        .list
    }

    def last_few_events()(implicit session: Session): Seq[String] = {
      val events = for {
        p <- Persons if p.visit_type === VisitType.Event
      } yield (p.host)

      events.sortBy(_.count.desc).list().distinct
    }

    def signout(needle: UUID)(implicit session: Session): Unit = {
      (for { p <- Persons if p.id === needle.bind } yield p.signed_out).update(true)
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