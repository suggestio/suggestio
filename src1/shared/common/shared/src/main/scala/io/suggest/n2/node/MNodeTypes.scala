package io.suggest.n2.node

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.{EnumeratumUtil, TreeEnumEntry}
import japgolly.univeq._
import play.api.libs.json.Format

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.10.17 11:51
  * Description: Enum-модель типов узлов N2: карточка, adn-узел, тег, картинка, видео, и т.д.
  */

object MNodeTypes extends StringEnum[MNodeType] {

  /** Юзер. */
  case object Person extends MNodeType("p")


  /** Узел ADN. */
  case object AdnNode extends MNodeType("n")


  /** Рекламная карточка. */
  case object Ad extends MNodeType("a")


  /** Теги/ключевые слова. */
  case object Tag extends MNodeType("t")


  /** Картинки, видео и т.д. */
  case object Media extends MNodeType("m") { that =>

    /** Загруженная картинка. */
    case object Image extends MNodeType("i") with _Child

    // TODO Video, Audio, Document, etc...

    /** Файл, не относящийся ни к картикам, ни к видео, ни к иным категориям из Media. */
    case object OtherFile extends MNodeType("mf") with _Child

    override def children: LazyList[MNodeType]  =
      Image #:: OtherFile #:: super.children

  }


  /** Маячок BLE. iBeacon или EddyStone -- системе это не важно. */
  case object BleBeacon extends MNodeType("b")


  /** Тип узла, описывающего некий абстрактный внешний http-ресурс. */
  case object ExternalRsc extends MNodeType("e") {

    /** Видео, обслуживаемое внешним видео-сервисом. */
    case object VideoExt extends MNodeType("ev") with _Child

    /** Просто какая-то внешняя страница (внешний ресурс) без какой-либо явной специализации.
      * Ресурс доступен по http/https, и нередко пригоден для использования через фрейм. */
    case object Resource extends MNodeType("er") with _Child

    override def children: LazyList[MNodeType] =
      VideoExt #:: Resource #:: super.children

  }


  /** Calendar type. Merged from model MCalendar. */
  case object Calendar extends MNodeType("calendar")

  /** Crypto.key type - storing key material inside node. */
  case object CryptoKey extends MNodeType("cryptoKey")

  case object WifiAP extends MNodeType( "wifi")

  override def values = findValues

  def adnTreeMemberTypes: List[MNodeType] = AdnNode :: Ad :: BleBeacon :: Nil

  /** Normal user can create nodes of these types (via lk-nodes): */
  def lkNodesUserCanCreate: List[MNodeType] =
    BleBeacon :: WifiAP :: Nil

}


/** Класс одного типа. */
sealed abstract class MNodeType(override val value: String)
  extends StringEnumEntry
  with TreeEnumEntry[MNodeType]


object MNodeType {

  /** Поддержка play-json. */
  implicit val MNODE_TYPE_FORMAT: Format[MNodeType] =
    EnumeratumUtil.valueEnumEntryFormat( MNodeTypes )

  @inline implicit def univEq: UnivEq[MNodeType] = UnivEq.derive


  implicit final class NodeTypeOpsExt( private val ntype: MNodeType ) extends AnyVal {

    /** Код сообщения в messages для единственного числа. */
    def singular = "Ntype." + ntype.value

    /** Код сообщений в messages для множественного числа. */
    def plural   = "Ntypes." + ntype.value

    /** Разрешается ли использовать рандомные id'шники? [true] */
    def randomIdAllowed: Boolean = {
      // Узлам-маячкам надо хранить свои uid'ы в _id. Так хоть и длинее,
      // но всё-таки нет необходимости в ведении ещё одного индекса.
      !_isBleBeacon
    }

    /**
      * Есть ли у юзера расширенный доступ к управлению узлом?
      * Это подразумевает возможность удалять узел и управлять значением isEnabled.
      */
    def userHasExtendedAcccess: Boolean = {
      // Юзер управляет маячками самостоятельно.
      _isBleBeacon
    }

    /** Разрешено ли неограниченному кругу лиц узнавать данные по узлу на тему размещения на нём? */
    def publicCanReadInfoAboutAdvOn: Boolean = {
      _isAdnNode ||
      // Маячок -- тоже узел для направленного размещения, с какими-то своими правилами.
      _isBleBeacon
    }

    /** Для этого типа узлов можно рендерить юзеру ссылку на личный кабинет? */
    def showGoToLkLink: Boolean =
      _isAdnNode

    def showScLink: Boolean =
      _isAdnNode || _isBleBeacon

    private def _isAdnNode =
      ntype ==* MNodeTypes.AdnNode

    private def _isBleBeacon =
      ntype ==* MNodeTypes.BleBeacon

    private def _isPerson =
      ntype ==* MNodeTypes.Person


    /** Юзер (через форму lk-nodes) может создавать дочерние узлы только для родительских узлов
      * разрешённых типов. */
    def userCanCreateSubNodes: Boolean =
      _isAdnNode || _isPerson

  }

}

