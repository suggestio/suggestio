package io.suggest.common.slick.driver

import com.github.tminglei.slickpg.{ExPostgresDriver, PgArraySupport, PgDateSupportJoda}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.11.15 9:38
 * Description: Slick-драйвер для postgres'а.
 * @see [[https://github.com/tminglei/slick-pg#usage]]
 */
trait ExPgSlickDriverT
  extends ExPostgresDriver
  with PgArraySupport
  with PgDateSupportJoda
{

  /** Реализация API расширенного slick-pg-драйвера. */
  trait ExPgApiT
    extends API
    with ArrayImplicits
    with JodaDateTimeImplicits
  {

    protected val _strArrayTypeMapper = new SimpleArrayJdbcType[String]("text")

    //implicit val strListTypeMapper    = _strArrayTypeMapper.to(_.toList)
    implicit val strSeqTypeMapper     = _strArrayTypeMapper.to(_.toSeq)

  }

  object ExPgApi extends ExPgApiT

  override val api = ExPgApi

}


/** Дефолтовая реализация slick-pg-драйвера. */
object ExPgSlickDriver extends ExPgSlickDriverT
