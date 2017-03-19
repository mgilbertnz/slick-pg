package com.github.tminglei.slickpg

import org.joda.time._
import org.joda.time.format.{DateTimeFormat, ISODateTimeFormat}
import org.postgresql.util.PGInterval
import slick.driver.PostgresDriver
import slick.jdbc.PositionedResult

trait PgDateSupportJoda extends date.PgDateExtensions with utils.PgCommonJdbcTypes { driver: PostgresDriver =>
  import PgJodaSupportUtils._
  import driver.api._

  /// alias
  trait DateTimeImplicits extends JodaDateTimeImplicits

  trait JodaDateTimeFormatters {
    val jodaDateFormatter = ISODateTimeFormat.date()
    val jodaTimeFormatter = DateTimeFormat.forPattern("HH:mm:ss.SSSSSS")
    val jodaTimeFormatter_NoFraction = DateTimeFormat.forPattern("HH:mm:ss")
    val jodaDateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSSSSS").withZoneUTC
    val jodaDateTimeFormatter_NoFraction = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss").withZoneUTC
    val jodaTzTimeFormatter = DateTimeFormat.forPattern("HH:mm:ss.SSSSSSZ")
    val jodaTzTimeFormatter_NoFraction = DateTimeFormat.forPattern("HH:mm:ssZ")
    val jodaTzDateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSSSSSZ").withZoneUTC
    val jodaTzDateTimeFormatter_NoFraction = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ssZ").withZoneUTC
  }

  trait JodaDateTimeImplicits extends JodaDateTimeFormatters {
    implicit val jodaDateTypeMapper = new GenericJdbcType[LocalDate]("date",
      LocalDate.parse(_, jodaDateFormatter), _.toString(jodaDateFormatter), hasLiteralForm=false)
    implicit val jodaTimeTypeMapper = new GenericJdbcType[LocalTime]("time",
      fnFromString = (s) => LocalTime.parse(s, if (s.indexOf(".") > 0) jodaTimeFormatter else jodaTimeFormatter_NoFraction),
      fnToString = (v) => v.toString(jodaTimeFormatter),
      hasLiteralForm = false)

    implicit val jodaInstantMapper = new GenericJdbcType[Instant]("timestamptz",
      fnFromString = (s) => Instant.parse(s,
        if (s.contains(".")) {
          if (s.contains("+")) { jodaTzDateTimeFormatter } else { jodaDateTimeFormatter }
        } else {
          if (s.contains("+")) { jodaTzDateTimeFormatter_NoFraction } else { jodaDateTimeFormatter_NoFraction }
        }),
      fnToString = (v) => v.toString(jodaTzDateTimeFormatter),
      hasLiteralForm = false)

    implicit val jodaDateTimeTypeMapper = new GenericJdbcType[LocalDateTime]("timestamp",
      fnFromString = (s) => LocalDateTime.parse(s, if (s.indexOf(".") > 0) jodaDateTimeFormatter else jodaDateTimeFormatter_NoFraction),
      fnToString = (v) => v.toString(jodaDateTimeFormatter),
      hasLiteralForm = false)
    implicit val jodaPeriodTypeMapper = new GenericJdbcType[Period]("interval",
      pgIntervalStr2jodaPeriod, hasLiteralForm=false)
    implicit val jodaTimestampTZTypeMapper = new GenericJdbcType[DateTime]("timestamptz",
      fnFromString = (s) => DateTime.parse(s,
        if (s.indexOf(":") > 2) { if (s.indexOf(".") > 0) jodaTzDateTimeFormatter else jodaTzDateTimeFormatter_NoFraction }
        else { if (s.indexOf(".") > 0) jodaTzTimeFormatter else jodaTzTimeFormatter_NoFraction }),
      fnToString = (v) => v.toString(jodaTzDateTimeFormatter),
      hasLiteralForm = false)

    ///
    implicit def jodaDateColumnExtensionMethods(c: Rep[LocalDate]) =
      new DateColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, Period, LocalDate](c)
    implicit def jodaDateOptColumnExtensionMethods(c: Rep[Option[LocalDate]]) =
      new DateColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, Period, Option[LocalDate]](c)

    implicit def jodaTimeColumnExtensionMethods(c: Rep[LocalTime]) =
      new TimeColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, DateTime, Period, LocalTime](c)
    implicit def jodaTimeOptColumnExtensionMethods(c: Rep[Option[LocalTime]]) =
      new TimeColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, DateTime, Period, Option[LocalTime]](c)

    implicit def jodaInstantColumnExtensionMethods(c: Rep[Instant]) =
      new TimestampColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, DateTime, Period, Instant](c)
    implicit def jodaInstantOptColumnExtensionMethods(c: Rep[Option[Instant]]) =
      new TimestampColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, DateTime, Period, Option[Instant]](c)

    implicit def jodaTimestampColumnExtensionMethods(c: Rep[LocalDateTime]) =
      new TimestampColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, DateTime, Period, LocalDateTime](c)
    implicit def jodaTimestampOptColumnExtensionMethods(c: Rep[Option[LocalDateTime]]) =
      new TimestampColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, DateTime, Period, Option[LocalDateTime]](c)

    implicit def jodaIntervalColumnExtensionMethods(c: Rep[Period]) =
      new IntervalColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, Period, Period](c)
    implicit def jodaIntervalOptColumnExtensionMethods(c: Rep[Option[Period]]) =
      new IntervalColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, Period, Option[Period]](c)

    implicit def jodaTzTimestampColumnExtensionMethods(c: Rep[DateTime]) =
      new TimestampColumnExtensionMethods[LocalDate, LocalTime, DateTime, LocalDateTime, Period, DateTime](c)
    implicit def jodaTzTimestampOptColumnExtensionMethods(c: Rep[Option[DateTime]]) =
      new TimestampColumnExtensionMethods[LocalDate, LocalTime, DateTime, LocalDateTime, Period, Option[DateTime]](c)
  }

  trait JodaDateTimePlainImplicits extends JodaDateTimeFormatters {
    import java.sql.Types

    import utils.PlainSQLUtils._

    import scala.reflect.classTag

    if (driver.isInstanceOf[ExPostgresDriver]) {
      driver.asInstanceOf[ExPostgresDriver].bindPgTypeToScala("date", classTag[LocalDate])
      driver.asInstanceOf[ExPostgresDriver].bindPgTypeToScala("time", classTag[LocalTime])
      driver.asInstanceOf[ExPostgresDriver].bindPgTypeToScala("timestamp", classTag[LocalDateTime])
      driver.asInstanceOf[ExPostgresDriver].bindPgTypeToScala("timestamptz", classTag[DateTime])
      // let users do it by themselves
//      driver.asInstanceOf[ExPostgresDriver].bindPgTypeToScala("interval", classTag[Duration])
//      driver.asInstanceOf[ExPostgresDriver].bindPgTypeToScala("interval", classTag[Period])
    }

    implicit class PgDate2TimePositionedResult(r: PositionedResult) {
      def nextLocalDate() = nextLocalDateOption().orNull
      def nextLocalDateOption() = r.nextStringOption().map(LocalDate.parse(_, jodaDateFormatter))
      def nextLocalTime() = nextLocalTimeOption().orNull
      def nextLocalTimeOption() = r.nextStringOption().map(s => LocalTime.parse(s,
        if(s.indexOf(".") > 0 ) jodaTimeFormatter else jodaTimeFormatter_NoFraction))
      def nextLocalDateTime() = nextLocalDateTimeOption().orNull
      def nextLocalDateTimeOption() = r.nextStringOption().map(s => LocalDateTime.parse(s,
        if(s.indexOf(".") > 0 ) jodaDateTimeFormatter else jodaDateTimeFormatter_NoFraction))
      def nextInstant() = nextInstantOption().orNull
      def nextInstantOption() = r.nextStringOption().map(s => Instant.parse(s,
        if(s.indexOf(".") > 0 ) jodaTzDateTimeFormatter else jodaTzDateTimeFormatter_NoFraction))
      def nextZonedDateTime() = nextZonedDateTimeOption().orNull
      def nextZonedDateTimeOption() = r.nextStringOption().map(s => DateTime.parse(s,
        if(s.indexOf(".") > 0 ) jodaTzDateTimeFormatter else jodaTzDateTimeFormatter_NoFraction))
      def nextPeriod() = nextPeriodOption().orNull
      def nextPeriodOption() = r.nextStringOption().map(pgIntervalStr2jodaPeriod)
    }

    /////////////////////////////////////////////////////////////////////////////
    implicit val getLocalDate = mkGetResult(_.nextLocalDate())
    implicit val getLocalDateOption = mkGetResult(_.nextLocalDateOption())
    implicit val setLocalDate = mkSetParameter[LocalDate]("date", _.toString(jodaDateFormatter), sqlType = Types.DATE)
    implicit val setLocalDateOption = mkOptionSetParameter[LocalDate]("date", _.toString(jodaDateFormatter), sqlType = Types.DATE)

    implicit val getLocalTime = mkGetResult(_.nextLocalTime())
    implicit val getLocalTimeOption = mkGetResult(_.nextLocalTimeOption())
    implicit val setLocalTime = mkSetParameter[LocalTime]("time", _.toString(jodaTimeFormatter), sqlType = Types.TIME)
    implicit val setLocalTimeOption = mkOptionSetParameter[LocalTime]("time", _.toString(jodaTimeFormatter), sqlType = Types.TIME)

    implicit val getLocalDateTime = mkGetResult(_.nextLocalDateTime())
    implicit val getLocalDateTimeOption = mkGetResult(_.nextLocalDateTimeOption())
    implicit val setLocalDateTime = mkSetParameter[LocalDateTime]("timestamp", _.toString(jodaDateTimeFormatter), sqlType = Types.TIMESTAMP)
    implicit val setLocalDateTimeOption = mkOptionSetParameter[LocalDateTime]("timestamp", _.toString(jodaDateTimeFormatter), sqlType = Types.TIMESTAMP)

    implicit val getInstant = mkGetResult(_.nextInstant())
    implicit val getInstantOption = mkGetResult(_.nextInstantOption())
    implicit val setInstant = mkSetParameter[Instant]("timestamp", _.toString(jodaTzDateTimeFormatter), sqlType = Types.TIMESTAMP)
    implicit val setInstantOption = mkOptionSetParameter[Instant]("timestamp", _.toString(jodaTzDateTimeFormatter), sqlType = Types.TIMESTAMP)

    implicit val getZonedDateTime = mkGetResult(_.nextZonedDateTime())
    implicit val getZonedDateTimeOption = mkGetResult(_.nextZonedDateTimeOption())
    implicit val setZonedDateTime = mkSetParameter[DateTime]("timestamptz", _.toString(jodaTzDateTimeFormatter), sqlType = Types.TIMESTAMP_WITH_TIMEZONE)
    implicit val setZonedDateTimeOption = mkOptionSetParameter[DateTime]("timestamptz", _.toString(jodaTzDateTimeFormatter), sqlType = Types.TIMESTAMP_WITH_TIMEZONE)

    implicit val getPeriod = mkGetResult(_.nextPeriod())
    implicit val getPeriodOption = mkGetResult(_.nextPeriodOption())
    implicit val setPeriod = mkSetParameter[Period]("interval")
    implicit val setPeriodOption = mkOptionSetParameter[Period]("interval")

    implicit val setDuration = mkSetParameter[Duration]("interval")
    implicit val setDurationOption = mkOptionSetParameter[Duration]("interval")
  }
}

object PgJodaSupportUtils {
  /// pg interval string --> joda Duration
  def pgIntervalStr2jodaPeriod(intervalStr: String): Period = {
    val pgInterval = new PGInterval(intervalStr)
    val seconds = Math.floor(pgInterval.getSeconds) .asInstanceOf[Int]
    val millis  = ((pgInterval.getSeconds - seconds) * 1000) .asInstanceOf[Int]

    new Period(
      pgInterval.getYears,
      pgInterval.getMonths,
      0,  // weeks
      pgInterval.getDays,
      pgInterval.getHours,
      pgInterval.getMinutes,
      seconds, millis
    )
  }
}
