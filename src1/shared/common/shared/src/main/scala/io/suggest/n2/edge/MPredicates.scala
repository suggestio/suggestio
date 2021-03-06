package io.suggest.n2.edge

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.{EnumeratumUtil, TreeEnumEntry}
import japgolly.univeq.UnivEq
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.09.15 10:32
  * Description: Статическая синхронная модель предикатов, т.е. "типов" ребер графа N2.
  * Создана по мотивам модели zotonic m_predicate, но сильно ушла от неё уже.
  */

/** Модель предикатов эджей. */
object MPredicates extends StringEnum[MPredicate] {

  // ------------------------------------------------------------------------
  // Экземпляры модели, идентификаторы идут по алфавиту: a->z, a1->z1, ...
  // ------------------------------------------------------------------------

  /** Субъект имеет право владения субъектом. */
  case object OwnedBy extends MPredicate("a")


  /**
    * Предикат создателя какого-то узла в системе. Обычно создатель -- это юзер или что-то такое.
    * from -- любой узел, например карточка или магазин.
    * to   -- юзер.
    */
  case object CreatedBy extends MPredicate("d")


  /** Предикат на юзера, выполнившего модерацию текущего узла.
    * Такой эдж модерации должен содержать инфу о результате модерации. */
  case object ModeratedBy extends MPredicate("g")


  /** Предикат для ресивера. Изначально, ресивером был узел (с ЛК), а объектом предиката -- рекламная карточка. */
  case object Receiver extends MPredicate("k") {

    /** Саморазмещение, т.е. ресивера, указывающего на продьюсера той же (текущей) карточки. */
    case object Self extends MPredicate("ks") with _Child

    /** Проплаченный узел-ресивер, купленный через подсистемы adv. */
    case object AdvDirect extends MPredicate("ka") with _Child

    override def children: LazyList[MPredicate] =
      Self #:: AdvDirect #:: super.children

  }


  /** Предикат указания на тег. */
  case object TaggedBy extends MPredicate("l") {

    /** Adv geo tags: платное размещение в гео-тегах. */
    case object AdvGeoTag extends MPredicate("lg") with _Child

    /** Узел сам-себе тег. В этом эдже лежит его tag payload: tag face'ы, гео-шейпы.
      * Все tag face'ы со всех узлов храняться в одном индексе, в т.ч. и этот.
      * В этом же эдже должна лежать пачка гео-шейпов со всех размещенных карточек. */
    case object Self extends MPredicate("ls") with _Child

    /**
      * Прямое размещение тега на каком-то узле.
      * Появилось как побочный продукт интеграции ресиверов в форму георазмещения.
      */
    case object DirectTag extends MPredicate("ld") with _Child

    /** Тег локации ADN-узла на карте.
      * Можно провести параллель с карточным AdvGeoTag, но для ADN-узла. */
    case object LocationTag extends MPredicate("ll") with _Child

    override def children: LazyList[MPredicate] =
      AdvGeoTag #:: Self #:: DirectTag #:: LocationTag #:: super.children

  }


  /**
    * Эдж для задания гео-шейпов геолокации узла в выдаче.
    *
    * SysAdnGeo, упавляющий геоформами вручную, исторически пишет в корневой предикат.
    * Поиск геолокации идёт всегда по корневому предикату.
    */
  case object NodeLocation extends MPredicate("n") {

    // TODO Сделать manual-эдж отдельными, а не корневым. Для этого надо обновление узлов произвести.
    // Можно "n"-эдж сделать дочерним для нового корневого, и запустить пересохранение всех узлов.

    /** 2017.may.17: Платные размещения на карте геолокации, подчиняются биллингу.
      * Выставляются и вычищаются биллингом в соотв. adv-билдере.
      */
    case object Paid extends MPredicate("np") with _Child

    override def children: LazyList[MPredicate] =
      Paid #:: super.children

  }


  /** Предикат для эджей, описывающих размещение карточек просто в шейпе на карте. */
  case object AdvGeoPlace extends MPredicate("o")


  /**
    * Что-то находится внутри чего-то.
    * Например, маячок "лежит" внутри ТЦ или магазина. Т.е. %предмет% -в-> %контейнере%.
    */
  case object PlacedIn extends MPredicate("p")


  /** Неявный эдж для jd-content'а, т.е. частей содержимого некоего json-документа.
    * Обычно не используется напрямую, а дёргаются конкретные дочерние эджи.
    * Появилось для кодирования эджей в jd-контексте.
    */
  case object JdContent extends MPredicate("s") {

    /** Эдж текста.
      * Исходная суть его в просто хранении текстового контента для json-doc-контента узла. */
    case object Text extends MPredicate("st") with _Child

    /** Картинка/изображение. */
    case object Image extends MPredicate("si") with _Child

    /** Видео-плеер, вёб-приложение или что-то ещё, живущее во фрейме (зависит от узла).
      * До 2018-08-23 здесь было только видео, поэтому v. */
    case object Frame extends MPredicate("sv") with _Child

    /** Эдж для отсылки к карточкам.
      * MEdge в nodeIds содержит id подкарточек.
      * jd-эдж содержит id карточки в nodeId.
      */
    case object Ad extends MPredicate("sa") with _Child


    override def children: LazyList[MPredicate] =
      Text #:: Image #:: Frame #:: Ad #:: super.children

  }


  /** Иденты (данные идентификации) однажды переехали сюда.
    * Эдж идента:
    * - nodeId -> email | id | phone | etc
    * - info.commenNi -> password | act_code
    * - info.flag -> проверенный ли идент?
    * - info.extService -> указатель на внешний сервис (MExtServices).
    *
    * Иденты переезжают в условиях перехода на гос.услуги, т.е. comment/flag-поля малопопулярны.
    */
  case object Ident extends MPredicate("q") {

    /** Адрес электронной почты. Если !flag, то commentNi содержит код активации почтового адреса. */
    case object Email extends MPredicate("qe") with _Child

    /** Номер телефона. Если !flag, то commentNi содержит смс-код активации номера. */
    case object Phone extends MPredicate("qp") with _Child

    /** Произвольный идентификатор (имя пользователя, постоянный id юзера на внешнем сервисе). */
    case object Id extends MPredicate("qi") with _Child

    /** Пароли хранятся отдельным эджем, чтобы была поддержка более гибких парольных систем
      * (память старого пароля, напоминание, подсказки, дата создания пароля, пароль для любого из (email,телефон,имя) и т.д.).
      * С другой стороны, переезд на гос.услуги намекает, что это всё не особо нужно, но кто знает...
      */
    case object Password extends MPredicate("qw") with _Child

    /** Language predicate.
      * Created for ability to transfer language information between different credentials storages.
      */
    case object Lang extends MPredicate("ql") with _Child

    override def children: LazyList[MPredicate] =
      Email #:: Phone #:: Id #:: Password #:: Lang #:: super.children

  }


  /** Предикат для эджа, описывающего файл (ссылку, id на уд.сервисе) приложения,
    * для скачивания на устройство пользователя.
    */
  case object Application extends MPredicate("i") {

    /** Файл установки приложения, доступный для скачивания.
      * Эдж указывает на узел-файл:
      *   e.media = None
      *   e.nodeId -> ["..esId.."]
      *     type=MNodeType.Media.* с File-эджем
      */
    case object FromFile extends MPredicate("iF") with _Child

    /** Какой-то сервис дистрибуции приложений: itunes, gplay, итд.
      * Сам сервис дистрибуции описывается в поле e.info.extService.
      * nodeId содержит id приложения на стороне сервиса дистрибуции и будет использовано для рендера URL.
      */
    case object Distributor extends MPredicate("iD") with _Child

    override def children: LazyList[MPredicate] =
      FromFile #:: Distributor #:: super.children

  }


  /** Блобик. Структура предикатов блоба напоминает структуру dom.Blob/dom.File . */
  case object Blob extends MPredicate("b") {

    /** Эдж описания файла в хранилище s.io.
      * Максимум один File-эдж на узел. Производные файлы сохраняются в связанные узлы со своими File-эджами.
      *
      * Тип файла - описывается вне эджа, тут только хранение этого абстрактного файла.
      * (есть MNodeTypes.Media.*, где можно описывать типы файлов-узлов).
      *
      * e.media = Some(...) для такого эджа всегда.
      * e.nodeIds = [] | [originalNodeId] - Узел-исходник файла, если это не оригинал.
      *
      * Если e.info.flags содержит InProgress, то значит файл ещё пока не готов.
      */
    case object File extends MPredicate("j") with _Child

    /** Описание куска данных (часть файла).
      * Слайс - блоб, являющаяся куском бОльшего блоба (chunk).
      * Chunk-эдж без порядкового номера содержит общую инфу по всей chunked-заливке, и разрешает chunk-заливку в узел.
      * Chunk-эдж может содержать инфу по storage-узлу, номеру chunk'а, byte offset (start, end), хэш-сумму конкретной части.
      */
    case object Slice extends MPredicate("bs") with _Child

    override def children: LazyList[MPredicate] =
      File #:: Slice #:: super.children

  }


  /** id для сборки Short URL. */
  case object ShortUrl extends MPredicate("u")


  /** Payout edge contains information about external payout target/account (bank card, wallet, etc) for receiving money.
    * info.paySystem should be defined.
    * info.payOut is defined.
    * doc.edgeUid may be defined.
    */
  case object PayoutData extends MPredicate("payoutData")


  /** Используется только в конструкторе, в тестах, в редкой sys edgeForm. */
  override def values = findValues

}


/** Базовый класс для каждого элемента модели [[MPredicates]]. */
sealed abstract class MPredicate(override val value: String)
  extends StringEnumEntry
  with TreeEnumEntry[MPredicate]


object MPredicate {

  /** Поддержка play-json. */
  implicit val MPREDICATE_FORMAT: Format[MPredicate] = {
    EnumeratumUtil.valueEnumEntryFormat( MPredicates )
  }

  /**
    * Запись предиката в список вместе с родительскими предикатами.
    * Это используется для полиморфной индексации (на стороне ES).
    *
    * Для связи между клиентом и сервером -- это в общем-то функция около-нулевой полезности.
    */
  val MPREDICATE_DEEP_FORMAT: Format[MPredicate] = {
    /** Сериализация в JSON, первый элемент -- текущий, второй и последующие -- родительские. */
    val PARENTRAL_WRITES: Writes[MPredicate] = {
      // Костыль из-за проблем contramap(). http://stackoverflow.com/a/27481370
      Writes[MPredicate] { mpred =>
        val p = implicitly[Writes[MPredicate]]
        val preds = mpred
          .meAndParents
          .map( p.writes )
        JsArray( preds )
      }
    }

    /** Десериализация из JSON-списка в первый элемент этого списка. */
    val PARENTRAL_READS: Reads[MPredicate] = {
      Reads[MPredicate] {
        case arr: JsArray =>
          if (arr.value.isEmpty)
            JsError("expected.nonempty.jsarray")
          else
            MPREDICATE_FORMAT.reads(arr.value.head)
        case str: JsString =>
          MPREDICATE_FORMAT.reads(str)
        case _ =>
          JsError("expected.jsstring.or.jsarray")
      }
    }

    Format(PARENTRAL_READS, PARENTRAL_WRITES)
  }

  @inline implicit def univEq: UnivEq[MPredicate] = UnivEq.derive


  implicit class PredicateOpsExt( private val pred: MPredicate ) extends AnyVal {

    /** Код i18n-сообщения с названием предиката в единственном числе. */
    def singular: String =
      "edge.predicate." + pred.value

  }

}
