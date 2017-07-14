package io.suggest.model.n2.edge

import io.suggest.common.menum.{EnumMaybeWithName, EnumTree}
import io.suggest.model.menum.EnumJsonReadsValT
import io.suggest.model.play.qsb.QueryStringBindableImpl
import play.api.libs.json._
import play.api.mvc.QueryStringBindable

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.09.15 10:32
  * Description: Статическая синхронная модель предикатов, т.е. "типов" ребер графа N2.
  * Создана по мотивам модели zotonic m_predicate, но сильно ушла от неё уже.
  */
object MPredicates extends EnumMaybeWithName with EnumJsonReadsValT with EnumTree {

  /** Трейт элемента модели. */
  protected sealed trait ValT extends super.ValT { that: T =>
    def singular = "edge.predicate." + strId
  }


  /** Класс одного элемента модели. */
  protected[this] sealed class Val(override val strId: String)
    extends super.Val(strId)
    with ValT
  { that =>

    /** Дочерние предикаты, если есть. */
    override def children: List[T] = Nil

    /** Родительский предикат, если есть. */
    override def parent: Option[T] = None

    /** Трейт для дочерних элементов. Они обычно наследуют черты родителей. */
    protected trait _Child { child: ValT =>
      override def parent: Option[T] = Some(that)
    }
  }

  override type T = Val


  /** Сериализация в JSON, первый элемент -- текущий, второй и последующие -- родительские. */
  val PARENTRAL_WRITES: Writes[T] = {
    // Костыль из-за проблем contramap(). http://stackoverflow.com/a/27481370
    Writes[T] { mpred =>
      val p = implicitly[Writes[T]]
      val preds = mpred
        .meAndParentsIterator
        .map { p.writes }
        .toSeq
      JsArray( preds )
    }
  }

  private def _READS = implicitly[Reads[T]]
  /** Десериализация из JSON-списка в первый элемент этого списка. */
  val PARENTRAL_READS: Reads[T] = {
    Reads[T] {
      case arr: JsArray =>
        if (arr.value.isEmpty)
          JsError("expected.nonempty.jsarray")
        else
          _READS.reads(arr.value.head)
      case str: JsString =>
        _READS.reads(str)
      case _ =>
        JsError("expected.jsstring.or.jsarray")
    }
  }

  /** compat-десериализация, поддерживает ввод как через meAndParents[], так и plain-предикат. */
  val PARENTAL_OR_DIRECT_READS: Reads[T] = {
    PARENTRAL_READS
      .orElse(_READS)
  }

  val PARENTAL_OR_DIRECT_FORMAT: Format[T] = {
    Format(PARENTAL_OR_DIRECT_READS, PARENTRAL_WRITES)
  }


  // ------------------------------------------------------------------------
  // Экземпляры модели, идентификаторы идут по алфавиту: a->z, a1->z1, ...
  // ------------------------------------------------------------------------

  /** Субъект имеет право владения субъектом. */
  val OwnedBy: T = new Val("a")

  /**
    * Предикат создателя какого-то узла в системе. Обычно создатель -- это юзер или что-то такое.
    * from -- любой узел, например карточка или магазин.
    * to   -- юзер.
    */
  val CreatedBy: T = new Val("d")


  /** Указание на картинку-логотип узла-учреждения.  */
  val Logo: T = new Val("e")


  /** Ребро указывает на родительский узел в географическом смысле.
    * Не обязательно это прямой гео-родитель. */
  val GeoParent = new Val("f") {

    /** Предикат прямого гео-родителя. */
    val Direct: T = new Val("g") with _Child

    override def children: List[T] = {
      Direct :: super.children
    }
  }


  /** Предикат, указывающий на логотип карточки приветствия.
    *
    * Изначально предикат назывался NodeWelcomeAdIs и хранил в себе id карточки приветствия в MWelcomeAd.
    * После ветки root:mad-to-n2 предикат стал использоваться для указания на логотип приветствия.
    * а неудавшаяся модель карточки приветствия окончательно отмерла.
    */
  val WcLogo: T = new Val("h")


  /** Предикат, направляемый в сторону картинки или иного объекта, являющегося предметом галлереи. */
  val GalleryItem: T = new Val("i")


  /** Предикат на юзера, выполнившего модерацию текущего узла.
    * Такой эдж модерации должен содержать инфу о результате модерации. */
  val ModeratedBy: T = new Val("j")


  /** Предикат для ресивера. Изначально, ресивером был узел (с ЛК), а объектом предиката -- рекламная карточка. */
  val Receiver = new Val("k") {

    /** Саморазмещение, т.е. ресивера, указывающего на продьюсера той же (текущей) карточки. */
    val Self: T = new Val("ks") with _Child

    /** Проплаченный узел-ресивер, купленный через подсистемы adv. */
    val AdvDirect: T = new Val("ka") with _Child

    override def children: List[T] = {
      Self :: AdvDirect :: super.children
    }
  }


  /** Предикат указания на тег. */
  val TaggedBy = new Val("l") {

    /** Adv geo tags: платное размещение в гео-тегах. */
    val Agt: T = new Val("lg") with _Child

    /** Узел сам-себе тег. В этом эдже лежит его tag payload: tag face'ы, гео-шейпы.
      * Все tag face'ы со всех узлов храняться в одном индексе, в т.ч. и этот.
      * В этом же эдже должна лежать пачка гео-шейпов со всех размещенных карточек. */
    val Self: T = new Val("ls") with _Child

    /**
      * Прямое размещение тега на каком-то узле.
      * Появилось как побочный продукт интеграции ресиверов в форму георазмещения.
      */
    val DirectTag: T = new Val("ld") with _Child

    override def children: List[T] = {
      Agt :: Self :: DirectTag :: super.children
    }

  }


  /** Фоновый объект по отношению к текущему объекту. */
  val Bg: T = new Val("m")


  /**
    * Эдж для задания гео-шейпов геолокации узла в выдаче.
    *
    * SysAdnGeo, упавляющий геоформами вручную, исторически пишет в корневой предикат.
    * Поиск геолокации идёт всегда по корневому предикату.
    */
  val NodeLocation = new Val("n") {

    // TODO Сделать manual-эдж отдельными, а не корневым. Для этого надо обновление узлов произвести.
    // Можно "n"-эдж сделать дочерним для нового корневого, и запустить пересохранение всех узлов.

    /** 2017.may.17: Платные размещения на карте геолокации, подчиняются биллингу.
      * Выставляются и вычищаются биллингом в соотв. adv-билдере.
      */
    val Paid: T = new Val("np") with _Child

    override def children = Paid :: super.children

  }


  /** Предикат для эджей, описывающих размещение карточек просто в шейпе на карте. */
  val AdvGeoPlace: T = new Val("o")

  /**
    * Что-то находится внутри чего-то.
    * Например, маячок "лежит" внутри ТЦ или магазина. Т.е. %предмет% -в-> %контейнере%.
    */
  val PlacedIn: T = new Val("p")

  /** Размещение ADN-узла на географической карте рекламополучателей. */
  @deprecated("Толком никакой смысловой нагрузки не было, и оно замёржено в NodeLocation", "2017-06-02")
  val AdnMap: T = new Val("r")


  /** Поддержка биндинга из routes. */
  implicit def mPredicateQsb(implicit strB: QueryStringBindable[String]): QueryStringBindable[T] = {
    new QueryStringBindableImpl[T] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, T]] = {
        for (strIdEith <- strB.bind(key, params)) yield {
          strIdEith.right.flatMap { strId =>
            maybeWithName(strId)
              .toRight("e.predicate.unknown")
          }
        }
      }
      override def unbind(key: String, value: T): String = {
        strB.unbind(key, value.strId)
      }
    }
  }

}
