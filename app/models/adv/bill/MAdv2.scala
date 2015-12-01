package models.adv.bill

import com.google.inject.{Inject, Singleton}
import io.suggest.ym.model.common.{SinkShowLevels, AdShowLevels}
import models.SinkShowLevel
import org.joda.time.DateTime
import slick.lifted.ProvenShape
import util.sqlm.slick.ExPgSlickDriverT

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.11.15 15:24
 * Description: Унифицированная модель биллинга рекламных размещений.
 * Пришла на смену MAdv (с её зоопарком подмоделей).
 * Пришла следом за MNode для узлов-карточек-картинок.
 * Модель содержит в себе кучу таблиц, но тут живёт в одном файле.
 * Свойства конкретного adv вынесены в подмодель.
 *
 * MAdv2_ содержит статическую сторону модели, инжектируемую через DI.
 * MAdv2T_ содержит трейт (интерфейс) в целях разруливания циркулярной инжекции компаньона в factoty.
 */

object MAdv2 {

  /** Имя корневой таблицы этой модели. */
  def TABLE_NAME = "adv2"

  def tableNameOrDlft(modeOpt: Option[MAdv2Mode]): String = {
    modeOpt.fold(TABLE_NAME)(_.tableName)
  }

  val ID_FN = "id"

  val MODE_FN               = "mode"
  val AD_ID_FN              = "ad_id"
  val DATE_START_FN         = "date_start"
  val DATE_END_FN           = "date_end"
  val PRODUCER_ID_FN        = "prod_id"
  val PROD_CONTRACT_ID_FN   = "prod_contract_id"
  val AMOUNT_FN             = "amount"
  val CURRENCY_CODE_FN      = "currency_code"
  val DATE_CREATED_FN       = "date_created"
  val DATE_STATUS_FN        = "date_status"
  val REASON_FN             = "reason"

  val RCVR_ID_FN = "rcvr_id"
  val SLS_FN     = "sls"

}


/** DI-контейнер для slick-таблиц adv2_*. */
@Singleton
class MAdv2Table @Inject() (
  val driver        : ExPgSlickDriverT
) {

  import driver.api._
  import MAdv2._

  /** Аддон для сборки классов таблиц adv2. */
  trait MAdv2sT extends Table[MAdv2] {

    def id              = column[Int](ID_FN, O.PrimaryKey, O.AutoInc)
    def mode            = column[String](MODE_FN, O.Length(1), O.SqlType("\"char\""))
    def adId            = column[String](AD_ID_FN, O.Length(32))
    def dateStart       = column[DateTime](DATE_START_FN)
    def dateEnd         = column[DateTime](DATE_END_FN)
    def producerId      = column[String](PRODUCER_ID_FN, O.Length(32))
    def prodContractId  = column[Int](PROD_CONTRACT_ID_FN)
    def amount          = column[Float](AMOUNT_FN)
    def currencyCode    = column[String](CURRENCY_CODE_FN, O.Length(3))
    def dateCreated     = column[DateTime](DATE_CREATED_FN, O.Default(DateTime.now))
    def dateStatus      = column[DateTime](DATE_STATUS_FN, O.Default(DateTime.now))
    def reason          = column[Option[String]](REASON_FN, O.Length(256))
    def rcvrId          = column[Option[String]](RCVR_ID_FN, O.Length(32))
    def sls             = column[Option[Seq[String]]](SLS_FN)

    /** Десериализация уровней отображения. */
    def toSlsSet(slsRaw: TraversableOnce[String]): Set[SinkShowLevel] = {
      slsRaw
        .toIterator
        .map { slRaw =>
          val result = if (slRaw.length == 1) {
            // compat: парсим slsPub, попутно конвертя их в sink-версии
            val sl = AdShowLevels.withName(slRaw)
            SinkShowLevels.fromAdSl(sl)
          } else {
            SinkShowLevels.withName(slRaw)
          }
          result : SinkShowLevel
        }
        .toSet
    }

    /** Сериализация уровней отображения. */
    def fromSlsSet(sls: Set[SinkShowLevel]): Seq[String] = {
      sls.iterator
        .map(_.name)
        .toSeq
    }

    /** Десериализация опциональных данных размещения в выдаче ресивера. */
    def toScRcvrOpt(rcvrIdOpt: Option[String], slsRawOpt: Option[Seq[String]]): Option[ScRcvr] = {
      for {
        rcvrId <- rcvrIdOpt
        slsRaw <- slsRawOpt
      } yield {
        ScRcvr(rcvrId, toSlsSet(slsRaw))
      }
    }

    /** Десериализация полного ряда таблицы. */
    def mapRow(idOpt: Option[Int], mode: String, adId: String, dateStart: DateTime, dateEnd: DateTime, producerId: String,
               prodContractId: Int, amount: Float, currencyCode: String, dateCreated: DateTime, dateStatus: DateTime,
               reason: Option[String], rcvrIdOpt: Option[String], slsRawOpt: Option[Seq[String]]): MAdv2 = {
      val scRcvrOpt = toScRcvrOpt(rcvrIdOpt, slsRawOpt)
      val common = MAdv2Common(
        mode            = MAdv2Modes.withName(mode),
        adId            = adId,
        dateStart       = dateStart,
        dateEnd         = dateEnd,
        producerId      = producerId,
        prodContractId  = prodContractId,
        amount          = amount,
        currencyCode    = currencyCode,
        dateCreated     = dateCreated,
        dateStatus      = dateStatus,
        reason          = reason
      )
      MAdv2(
        common = common,
        scRcvr = scRcvrOpt,
        id     = idOpt
      )
    }

    /** Сериализация ряда таблицы. */
    def unmapRow(m: MAdv2) = {
      val c = m.common
      val rcvrIdOpt = m.scRcvr.map(_.rcvrId)
      val slsRawOpt = m.scRcvr.map { scr =>
        fromSlsSet(scr.sls)
      }
      val t = (m.id, c.mode.strId, c.adId, c.dateStart, c.dateEnd, c.producerId, c.prodContractId,
        c.amount, c.currencyCode, c.dateCreated, c.dateStatus, c.reason, rcvrIdOpt, slsRawOpt)
      Some(t)
    }

    /** Дефолтовая проекция ряда табилцы. */
    override def * : ProvenShape[MAdv2] = {
      (id.?, mode, adId, dateStart, dateEnd, producerId, prodContractId, amount, currencyCode, dateCreated, dateStatus,
        reason, rcvrId, sls) <> ((mapRow _).tupled, unmapRow)
    }

  }


  // Базовый класс для slick-таблиц.
  class MAdv2s(tag: Tag, tmode: Option[MAdv2Mode] = None)
    extends Table[MAdv2](tag, MAdv2.tableNameOrDlft(tmode))
    with MAdv2sT


  class TAll(tag: Tag) extends MAdv2s(tag, None)
  class TReq(tag: Tag) extends MAdv2s(tag, Some(MAdv2Modes.Req))
  class TApproved(tag: Tag) extends MAdv2s(tag, Some(MAdv2Modes.Approved))
  class TOnline(tag: Tag) extends MAdv2s(tag, Some(MAdv2Modes.Online))
  class TDone(tag: Tag) extends MAdv2s(tag, Some(MAdv2Modes.Finished))
  // Rejected объединен с Finished.

}


/** Динамический экземпляр модели. */
case class MAdv2(
  common        : MAdv2Common,
  scRcvr        : Option[ScRcvr]  = None,
  id            : Option[Int]     = None
)
