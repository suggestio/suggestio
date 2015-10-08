package io.suggest.model.n2.edge

import io.suggest.common.menum.{EnumTree, EnumMaybeWithName}
import io.suggest.model.menum.EnumJsonReadsValT
import io.suggest.model.n2.node.{MNodeTypes, MNodeType}
import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.09.15 10:32
 * Description: Статическая синхронная модель предикатов, т.е. "типов" ребер графа N2.
 * Создана по мотивам модели zotonic m_predicate.
 */
object MPredicates extends EnumMaybeWithName with EnumJsonReadsValT with EnumTree {

  /** Трейт элемента модели. */
  protected sealed trait ValT extends super.ValT { that: T =>

    /** Типы узлов, которые могут выступать субъектом данного предиката. */
    def fromTypeValid(ntype: MNodeType): Boolean

    /** Типы узлов, которые могут выступать объектами данного предиката. */
    def toTypeValid(ntype: MNodeType): Boolean

  }


  /** Класс одного элемента модели. */
  protected[this] abstract sealed class Val(override val strId: String)
    extends super.Val(strId)
    with ValT
  {

    /** Дочерние предикаты, если есть. */
    override def children: List[T] = Nil

    /** Родительский предикат, если есть. */
    override def parent: Option[T] = None

  }

  override type T = Val


  // Короткие врапперы для определения принадлежности типов друг к другу.
  private def _isAdnNode(ntype: MNodeType): Boolean = {
    ntype ==>> MNodeTypes.AdnNode
  }
  private def _isPerson(ntype: MNodeType): Boolean = {
    ntype ==>> MNodeTypes.Person
  }
  private def _isAd(ntype: MNodeType): Boolean = {
    ntype ==>> MNodeTypes.Ad
  }
  private def _isImage(ntype: MNodeType): Boolean = {
    ntype ==>> MNodeTypes.Media.Image
  }
  private def _isWcAd(ntype: MNodeType): Boolean = {
    ntype ==>> MNodeTypes.WelcomeAd
  }

  protected sealed trait _FromAdnNode extends ValT { that: T =>
    override def fromTypeValid(ntype: MNodeType): Boolean = {
      _isAdnNode(ntype)
    }
  }
  protected sealed trait _ToAdnNode extends ValT { that: T =>
    override def toTypeValid(ntype: MNodeType): Boolean = {
      _isAdnNode(ntype)
    }
  }

  protected sealed trait _ToImg extends ValT { that: T =>
    override def toTypeValid(ntype: MNodeType): Boolean = {
      _isImage(ntype)
    }
  }


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
      case other =>
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
  val OwnedBy: T = new Val("a") {
    override def fromTypeValid(ntype: MNodeType) = !_isPerson(ntype)
    override def toTypeValid(ntype: MNodeType)   = true
  }


  /**
   * Модерация запросов на размещение с from-узла делегирована другому указанному узлу.
   * from -- узел, входящие запросы на который будут обрабатываться узлом-делегатом
   * to   -- узел-делегат, который видит у себя запросы на размещение от других узлов.
   */
  val AdvMdrDgTo: T = new Val("c") with _FromAdnNode with _ToAdnNode


  /**
   * Предикат юзера-создателя какого-то узла в системе.
   * from -- юзер.
   * to   -- любой узел, например карточка или магазин.
   */
  val CreatorOf: T = new Val("d") {
    override def fromTypeValid(ntype: MNodeType): Boolean = {
      _isPerson(ntype)
    }
    override def toTypeValid(ntype: MNodeType): Boolean = {
      true
    }
  }


  /** Указание на картинку-логотип узла-учреждения.  */
  val Logo: T = new Val("e") with _FromAdnNode with _ToImg


  /** Ребро указывает на родительский узел в географическом смысле.
    * Не обязательно это прямой гео-родитель. */
  val GeoParent = new Val("f") with _FromAdnNode with _ToAdnNode { geoParent =>

    protected sealed trait _Parent extends ValT { that: T =>
      override def parent: Option[T] = Some(geoParent)
    }

    /** Предикат прямого гео-родителя. */
    val Direct: T = new Val("g") with _FromAdnNode with _ToAdnNode with _Parent

    override def children: List[T] = {
      Direct :: super.children
    }
  }

  /** Предикат, указывающий на карточку приветствия. */
  val NodeWelcomeAdIs = new Val("h") with _FromAdnNode {
    override def toTypeValid(ntype: MNodeType) = _isWcAd(ntype)
  }

  /** Предикат, направляемый в сторону картинки или иного объекта, являющегося предметом галлереи. */
  val GalleryItem = new Val("i") with _FromAdnNode with _ToImg

}
