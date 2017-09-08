package io.suggest.model.n2.edge

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.{EnumeratumUtil, TreeEnumEntry}
import io.suggest.primo.IStrId
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.09.15 10:32
  * Description: Статическая синхронная модель предикатов, т.е. "типов" ребер графа N2.
  * Создана по мотивам модели zotonic m_predicate, но сильно ушла от неё уже.
  */
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
          .meAndParentsIterator
          .map { p.writes }
          .toSeq
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

}


/** Базовый класс для каждого элемента модели [[MPredicates]]. */
sealed abstract class MPredicate(override val value: String)
  extends StringEnumEntry
  with TreeEnumEntry[MPredicate]
  with IStrId
{ that: MPredicate =>

  /** Некий строковой ключ. Например, ключ элемента модели. */
  // TODO Выкинуть strId?
  @inline override final def strId: String = value

  /** Код i18n-сообщения с названием предиката в единственном числе. */
  def singular: String = {
    "edge.predicate." + strId
  }

}



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


  /** Указание на картинку-логотип узла-учреждения.  */
  case object Logo extends MPredicate("e")


  /** Ребро указывает на родительский узел в географическом смысле.
    * Не обязательно это прямой гео-родитель. */
  case object GeoParent extends MPredicate("f") {

    /** Предикат прямого гео-родителя. */
    case object Direct extends MPredicate("g") with _Child

    override def children: List[MPredicate] = {
      Direct :: super.children
    }

  }


  /** Предикат, указывающий на логотип карточки приветствия.
    *
    * Изначально предикат назывался NodeWelcomeAdIs и хранил в себе id карточки приветствия в MWelcomeAd.
    * После ветки root:mad-to-n2 предикат стал использоваться для указания на логотип приветствия.
    * а неудавшаяся модель карточки приветствия окончательно отмерла.
    */
  case object WcLogo extends MPredicate("h")


  /** Предикат, направляемый в сторону картинки или иного объекта, являющегося предметом галлереи. */
  case object GalleryItem extends MPredicate("i")


  /** Предикат на юзера, выполнившего модерацию текущего узла.
    * Такой эдж модерации должен содержать инфу о результате модерации. */
  case object ModeratedBy extends MPredicate("j")


  /** Предикат для ресивера. Изначально, ресивером был узел (с ЛК), а объектом предиката -- рекламная карточка. */
  case object Receiver extends MPredicate("k") {

    /** Саморазмещение, т.е. ресивера, указывающего на продьюсера той же (текущей) карточки. */
    case object Self extends MPredicate("ks") with _Child

    /** Проплаченный узел-ресивер, купленный через подсистемы adv. */
    case object AdvDirect extends MPredicate("ka") with _Child

    override def children: List[MPredicate] = {
      Self :: AdvDirect :: super.children
    }

  }


  /** Предикат указания на тег. */
  case object TaggedBy extends MPredicate("l") {

    /** Adv geo tags: платное размещение в гео-тегах. */
    case object Agt extends MPredicate("lg") with _Child

    /** Узел сам-себе тег. В этом эдже лежит его tag payload: tag face'ы, гео-шейпы.
      * Все tag face'ы со всех узлов храняться в одном индексе, в т.ч. и этот.
      * В этом же эдже должна лежать пачка гео-шейпов со всех размещенных карточек. */
    case object Self extends MPredicate("ls") with _Child

    /**
      * Прямое размещение тега на каком-то узле.
      * Появилось как побочный продукт интеграции ресиверов в форму георазмещения.
      */
    case object DirectTag extends MPredicate("ld") with _Child

    override def children: List[MPredicate] = {
      Agt :: Self :: DirectTag :: super.children
    }

  }


  /** Фоновый объект по отношению к текущему объекту. */
  case object Bg extends MPredicate("m")


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

    override def children: List[MPredicate] = {
      Paid :: super.children
    }

  }


  /** Предикат для эджей, описывающих размещение карточек просто в шейпе на карте. */
  case object AdvGeoPlace extends MPredicate("o")


  /**
    * Что-то находится внутри чего-то.
    * Например, маячок "лежит" внутри ТЦ или магазина. Т.е. %предмет% -в-> %контейнере%.
    */
  case object PlacedIn extends MPredicate("p")


  /** Размещение ADN-узла на географической карте рекламополучателей. */
  @deprecated("Толком никакой смысловой нагрузки не было, и оно замёржено в NodeLocation", "2017-06-02")
  case object AdnMap extends MPredicate("r")


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

    /** Видео. */
    case object Video extends MPredicate("sv") with _Child

    override def children: List[MPredicate] = {
      Text :: Image :: Video :: super.children
    }

  }


  override val values = {
    findValues.flatMap { v =>
      v :: v.deepChildren
    }
  }

}
