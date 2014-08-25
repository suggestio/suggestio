package models

import anorm._
import org.joda.time.{Period, LocalDate, DateTime}
import util.AnormJodaTime._
import util.AnormPgArray._
import util.AnormPgInterval._
import java.sql.Connection
import java.util.Currency

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.05.14 16:59
 * Description: Модель для группы родственных таблиц adv_*.
 */
object MAdv {
  import SqlParser._

  val ADV_MODE_PARSER = get[String]("mode").map(MAdvModes.withName)
  val AMOUNT_PARSER = get[Float]("amount")
  val CURRENCY_CODE_PARSER = get[String]("currency_code")
  val CURRENCY_PARSER = CURRENCY_CODE_PARSER.map { Currency.getInstance }
  val PROD_ADN_ID_PARSER = get[String]("prod_adn_id")
  val AD_ID_PARSER = str("ad_id")

  /** Базовый парсер для колонок таблиц adv_* для колонок, которые идут слева, т.е. появились до создания дочерних таблиц. */
  val ADV_ROW_PARSER_1 = get[Option[Int]]("id") ~ AD_ID_PARSER ~ AMOUNT_PARSER ~ CURRENCY_CODE_PARSER ~
    get[DateTime]("date_created") ~ get[Option[Float]]("comission") ~ ADV_MODE_PARSER ~
    get[DateTime]("date_start") ~ get[DateTime]("date_end") ~ PROD_ADN_ID_PARSER ~ get[String]("rcvr_adn_id")

  /** Парсер для значений в колонке showLevels. Там массив с уровнями отображения.
    * Изначально были AdShowLevels, потом стали SinkShowLevels. */
  val SHOW_LEVELS_PARSER = get[Set[String]]("show_levels") map { slsRaw =>
    slsRaw.map { slRaw =>
      val result = if (slRaw.length == 1) {
        // compat: парсим slsPub, попутно конвертя их в sink-версии
        val sl = AdShowLevels.withName(slRaw)
        SinkShowLevels.fromAdSl(sl)
      } else {
        SinkShowLevels.withName(slRaw)
      }
      result : SinkShowLevel
    }
  }

  def ADV_ROW_PARSER_2 = SHOW_LEVELS_PARSER

  val COUNT_PARSER = get[Long]("c")

  implicit def modes2strs(modes: Traversable[MAdvMode]): Traversable[String] = {
    modes.map(_.toString)
  }

  /**
   * Найти все ad_id (id рекламных карточек), ряды которых имеют указанные режимы и которые ещё не
   * истекли по date_end.
   * @param modes Режимы, по которым искать-перебирать.
   * @return Список ad_id в неопределённом порядке, но без дубликатов.
   */
  def findAllNonExpiredAdIdsForModes(modes: Set[MAdvMode])(implicit c: Connection): List[String] = {
    SQL("SELECT DISTINCT ad_id FROM adv WHERE mode = ANY({modes}) AND date_end <= now()")
      .on('modes -> strings2pgArray(modes))
      .as(AD_ID_PARSER *)
  }


  /**
   * Найти все id рекламных карточек, которые относятся к актуальным рядам между указанными
   * продьюсером и ресивером.
   * @param modes в каких таблицах искать.
   * @param prodId id продьюсера.
   * @param rcvrId id ресивера.
   * @return Список ad_id без дубликатов в неопределённом порядке.
   */
  def findActualAdIdsBetweenNodes(modes: Set[MAdvMode], prodId: String, rcvrId: String)(implicit c: Connection): List[String] = {
    SQL("SELECT DISTINCT ad_id FROM adv WHERE mode = ANY({modes}) AND prod_adn_id = {prodId} AND rcvr_adn_id = {rcvrId} AND date_end >= now()")
      .on('modes -> strings2pgArray(modes), 'prodId -> prodId, 'rcvrId -> rcvrId)
      .as(MAdv.AD_ID_PARSER *)
  }
}


/** Интерфейс всех экземпляров MAdv* моделей. */
trait MAdvI extends CurrencyCode { madvi =>
  def adId          : String
  def amount        : Float
  def comission     : Option[Float]
  def dateCreated   : DateTime
  def id            : Option[Int]
  def mode          : MAdvMode
  def dateStatus    : DateTime
  def dateStart     : DateTime
  def dateEnd       : DateTime
  def prodAdnId     : String
  def rcvrAdnId     : String
  def showLevels    : Set[SinkShowLevel]

  def amountMinusComission: Float = comission.fold(amount)(comission => amount * (1.0F - comission))
  def comissionAmount: Float =  comission.fold(amount)(amount * _)
  def advTerms = new AdvTerms {
    override def showLevels = madvi.showLevels
    override def dateEnd: LocalDate = madvi.dateStart.toLocalDate
    override def dateStart: LocalDate = madvi.dateEnd.toLocalDate
  }

  def maybeOk = this match {
    case madvOk: MAdvOk => Some(madvOk)
    case _ => None
  }

  def maybeRefused = this match {
    case madvRef: MAdvRefuse => Some(madvRef)
    case _ => None
  }

  def maybeReq = this match {
    case madvReq: MAdvReq => Some(madvReq)
    case _ => None
  }

  def isOk      = mode == MAdvModes.OK
  def isReq     = mode == MAdvModes.REQ
  def isRefused = mode == MAdvModes.REFUSED


  def hasOnAdSl(sl: AdShowLevel): Boolean = showLevels.exists(_.sl == sl)
}


object MAdvModes extends Enumeration {
  type MAdvMode = Value
  val OK      = Value("o")
  val REQ     = Value("r")
  val REFUSED = Value("e")

  def busyModes = Set(OK, REQ)
}


trait MAdvStatic extends SqlModelStatic {

  override type T <: MAdvI

  def getActualById(id: Int, policy: SelectPolicy = SelectPolicies.NONE)(implicit c: Connection) = {
    getByIdBase(id, policy, Some("AND date_end >= now()"))
  }

  /**
   * Поиск по колонке adId, т.е. по id рекламной карточки.
   * @param adId id рекламной карточки, которую размещают.
   * @return Список найленных рядов в неопределённом порядке.
   */
  def findByAdId(adId: String, limit: Int = 100, policy: SelectPolicy = SelectPolicies.NONE)(implicit c: Connection): List[T] = {
    findBy(" WHERE ad_id = {adId} LIMIT {limit}", policy, 'adId -> adId, 'limit -> limit)
  }

  /**
   * Найти все ряды, которые относятся к указанной рекламной карточке и date_end ещё пока в будущем.
   * @param adId id рекламной карточки.
   * @return Список подходящих под запрос рядов в произвольном порядке.
   */
  def findNotExpiredByAdId(adId: String, policy: SelectPolicy = SelectPolicies.NONE, limit: Int = 100)(implicit c: Connection): List[T] = {
    findBy(" WHERE ad_id = {adId} AND date_end >= now() LIMIT {limit}", policy, 'adId -> adId, 'limit -> limit)
  }

  def findNotExpiredRelatedTo(adnId: String, policy: SelectPolicy = SelectPolicies.NONE)(implicit c: Connection): List[T] = {
    findBy(" WHERE (prod_adn_id = {adnId} OR rcvr_adn_id = {adnId}) AND date_end >= now()", policy, 'adnId -> adnId)
  }

  /**
   * Найти все id рекламных карточек, которые относятся к актуальным рядам между указанными
   * продьюсером и ресивером.
   * @param prodId id продьюсера.
   * @param rcvrId id ресивера.
   * @return Список ad_id без дубликатов в неопределённом порядке.
   */
  def findActualAdIdsBetweenNodes(prodId: String, rcvrId: String)(implicit c: Connection): List[String] = {
    SQL("SELECT DISTICT ad_id FROM " + TABLE_NAME + " WHERE prod_adn_id = {prodId} AND rcvr_adn_id = {rcvrId} AND date_end >= now()")
      .on('prodId -> prodId, 'rcvrId -> rcvrId)
      .as(MAdv.AD_ID_PARSER *)
  }

  /**
   * Есть ли в таблице ряды, которые относятся к указанной комбинации adId и rcvrId, и чтобы были
   * действительны по времени.
   * @param adId id рекламной карточки.
   * @param rcvrId id узла-ресивера.
   * @return true, если есть хотя бы один актуальный ряд для указанных adId и rcvrId. Иначе false.
   */
  def hasNotExpiredByAdIdAndRcvr(adId: String, rcvrId: String)(implicit c: Connection): Boolean = {
    SQL("SELECT count(*) > 0 AS bool FROM " + TABLE_NAME + " WHERE ad_id = {adId} AND rcvr_adn_id = {rcvrId} AND now() <= date_end LIMIT 1")
      .on('adId -> adId, 'rcvrId -> rcvrId)
      .as(SqlModelStatic.boolColumnParser single)
  }

  def findNotExpiredByAdIdAndRcvr(adId: String, rcvrId: String, policy: SelectPolicy = SelectPolicies.NONE)(implicit c: Connection): List[T] = {
    findBy(" WHERE ad_id = {adId} AND rcvr_adn_id = {rcvrId} AND now() <= date_end", policy, 'adId -> adId, 'rcvrId -> rcvrId)
  }

  /**
   * Найти ряды по карточке и адресату запроса размещения.
   * @param adId id карточки.
   * @param rcvrId id получателя.
   * @param policy Политика блокировок.
   * @return Список подходящих рядов в неопределённом порядке.
   */
  def findByAdIdAndRcvr(adId: String, rcvrId: String, policy: SelectPolicy = SelectPolicies.NONE, limit: Option[Int] = None)(implicit c: Connection): List[T] = {
    var sql1 = " WHERE ad_id = {adId} AND rcvr_adn_id = {rcvrId}"
    var args1: List[NamedParameter] = List('adId -> adId, 'rcvrId -> rcvrId)
    if (limit.isDefined) {
      sql1 += " LIMIT {limit}"
      args1 ::= 'limit -> limit.get
    }
    findBy(sql1, policy, args1 : _*)
  }

  def findByAdIdAndRcvrs(adId: String, rcvrIds: Traversable[String], policy: SelectPolicy = SelectPolicies.NONE)(implicit c: Connection): List[T] = {
    findBy(" WHERE ad_id = {adId} AND rcvr_adn_id = ANY({rcvrIds})", policy, 'rcvrIds -> strings2pgArray(rcvrIds), 'ad_id -> adId)
  }

  /** Найти все ряды, поля adId и rcvrAdnId которых одновременно лежат в указанных множествах. */
  def findByAdIdsAndRcvrs(adIds: Traversable[String], rcvrIds: Traversable[String],
                          policy: SelectPolicy = SelectPolicies.NONE)(implicit c: Connection): List[T] = {
    findBy(
      " WHERE ad_id = ANY({adIds}) AND rcvr_adn_id = ANY({rcvrIds})", policy,
      'adIds -> strings2pgArray(adIds), 'rcvrIds -> strings2pgArray(rcvrIds)
    )
  }

  def findByAdIdsAndProducers(adIds: Traversable[String], prodIds: Traversable[String],
                              policy: SelectPolicy = SelectPolicies.NONE)(implicit c: Connection): List[T] = {
    findBy(
      " WHERE ad_id = ANY({adIds}) AND prod_adn_id = ANY({prodIds})", policy,
      'adIds -> strings2pgArray(adIds), 'prodIds -> strings2pgArray(prodIds)
    )
  }

  /** Найти все ряда, содержащие указанного получателя в соотв. колонке.
    * @param rcvrAdnId id узла-получателя.
    * @param policy Политика блокировки рядов.
    * @return Список рядов, адресованных указанному получателю, в неопр.порядке.
    */
  def findByRcvr(rcvrAdnId: String, policy: SelectPolicy = SelectPolicies.NONE)(implicit c: Connection): List[T] = {
    findBy(" WHERE rcvr_adn_id = {rcvrAdnId}", policy, 'rcvrAdnId -> rcvrAdnId)
  }

  /**
   * Аналог findByRcvr(), но для множества ресиверов.
   * @param rcvrAdnIds id искомых ресиверов.
   * @param policy Политика блокирования выбранных рядов. По умолчанию - без локов.
   * @return Список рядов, имеющих любого из указанных ресиверов в поле rcvr_adn_id, в неопределённом порядке.
   */
  def findByRcvrs(rcvrAdnIds: Traversable[String], policy: SelectPolicy = SelectPolicies.NONE)(implicit c: Connection): List[T] = {
    findBy(" WHERE rcvr_adn_id = ANY({rcvrs})", policy, 'rcvrs -> strings2pgArray(rcvrAdnIds))
  }

  /**
   * Есть ли в текущей adv-модели ряд, который относится к указанной рекламной карточке
   * @param adId id рекламной карточки.
   * @return true, если в таблице есть хотя бы один подходящий ряд.
   */
  def hasAdvUntilNow(adId: String)(implicit c: Connection): Boolean = {
    SQL("SELECT count(*) > 0 AS bool FROM " + TABLE_NAME + " WHERE ad_id = {adId} AND date_end >= now() LIMIT 1")
      .on('adId -> adId)
      .as(SqlModelStatic.boolColumnParser single)
  }

  /**
   * Найти всех продьюсеров (рекламодателей) для указанного ресивера.
   * @param rcvrAdnId adn id узла-ресивера.
   * @return Список adn id продьюсеров (узлов-рекламодателей) в неопределённом порядке
   */
  def findAllProducersForRcvr(rcvrAdnId: String)(implicit c: Connection): List[String] = {
    SQL("SELECT DISTINCT prod_adn_id FROM " + TABLE_NAME + " WHERE rcvr_adn_id = {rcvrAdnId} AND date_end >= now()")
     .on('rcvrAdnId -> rcvrAdnId)
     .as(MAdv.PROD_ADN_ID_PARSER *)
  }
  def findAllProducersForRcvrs(rcvrAdnIds: Traversable[String])(implicit c: Connection): List[String] = {
    SQL("SELECT DISTINCT prod_adn_id FROM " + TABLE_NAME + " WHERE rcvr_adn_id = ANY({rcvrs}) AND date_end >= now()")
     .on('rcvrs -> strings2pgArray(rcvrAdnIds))
     .as(MAdv.PROD_ADN_ID_PARSER *)
  }

  /**
   * Посчитать кол-во рядов, относящихся к указанному ресиверу.
   * @param rcvrAdnId adn id узла-ресивера.
   * @return Неотрицательно кол-во.
   */
  def countForRcvr(rcvrAdnId: String)(implicit c: Connection): Long = {
    SQL("SELECT count(*) AS c FROM " + TABLE_NAME + " WHERE rcvr_adn_id = {rcvrAdnId}")
      .on('rcvrAdnId -> rcvrAdnId)
      .as(MAdv.COUNT_PARSER single)
  }
  def countForRcvrs(rcvrAdnIds: Traversable[String])(implicit c: Connection): Long = {
    SQL("SELECT count(*) AS c FROM " + TABLE_NAME + " WHERE rcvr_adn_id = ANY({rcvrs})")
      .on('rcvrs -> strings2pgArray(rcvrAdnIds))
      .as(MAdv.COUNT_PARSER single)
  }

  /**
   * Фильтрация по колонке date_created. Поиск всех рядов, которые созданы не ранее последнего времени.
   * @param createdInPeriod Период относительно now(), в течение которого ряды отбрасываются.
   * @return Список рядов, которые уже созданы и существуют не менее указанного периода.
   */
  def findCreatedLast(createdInPeriod: Period, policy: SelectPolicy = SelectPolicies.NONE)(implicit c: Connection): List[T] = {
    findBy(" WHERE date_created + {createdInPeriod} <= now()", policy, 'createdInPeriod -> createdInPeriod)
  }

  /** Найти последнюю актуальную запись, касающуюся указанной рекламной карточки, размещаемой на указанном ресивере.
    * @param adId id рекламной карточки.
    * @param rcvrId id ресивера.
    * @return Опциональный результат.
    */
  def getLastActualByAdIdRcvr(adId: String, rcvrId: String)(implicit c: Connection): Option[T] = {
    SQL("SELECT * FROM " + TABLE_NAME + " WHERE ad_id = {adId} AND rcvr_adn_id = {rcvrId} AND date_end >= now() ORDER BY id DESC LIMIT 1")
      .on('adId -> adId, 'rcvrId -> rcvrId)
      .as(rowParser *)
      .headOption
  }
}


/** Условия размещения с точки зрения юзера. */
trait AdvTerms {
  def showLevels: Set[SinkShowLevel]
  def dateStart: LocalDate
  def dateEnd: LocalDate
}
