package io.suggest.model.n2.node

import io.suggest.common.empty.EmptyUtil
import io.suggest.common.menum.{EnumJsonReadsValT, EnumMaybeWithName, EnumTree}
import io.suggest.model.play.qsb.QueryStringBindableImpl
import play.api.mvc.QueryStringBindable

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.09.15 11:51
 * Description: Синхронная статическая модель типов узлов N2: карточка, adn-узел, тег, картинка, видео, и т.д.
 * В рамках зотоника была динамическая модель m_category.
 * В рамках s.io нет нужды в такой тяжелой модели, т.к. от категорий мы уже ушли к тегам.
 */
object MNodeTypes extends EnumMaybeWithName with EnumJsonReadsValT with EnumTree {

  /** Трейт каждого элемента данной модели. */
  protected sealed trait ValT
    extends super.ValT
  { that: T =>

    /** Код сообщения в messages для единственного числа. */
    def singular = "Ntype." + strId

    /** Код сообщений в messages для множественного числа. */
    def plural   = "Ntypes." + strId

    /** Логика генерации ntype-specific отображаемого имени для узла. */
    def guessNodeDisplayName(mnode: MNode): Option[String] = None

    /** Разрешается ли использовать рандомные id'шники? [true] */
    def randomIdAllowed: Boolean = true

    /**
      * Есть ли у юзера расширенный доступ к управлению узлом?
      * Это подразумевает возможность удалять узел и управлять значением isEnabled.
      */
    def userHasExtendedAcccess: Boolean = false

    /** Разрешено ли неограниченному кругу лиц узнавать данные по узлу на тему размещения на нём? */
    def publicCanReadInfoAboutAdvOn: Boolean = false

  }


  /** Абстрактная класс одного элемента модели. */
  protected[this] abstract class Val(override val strId: String)
    extends super.Val(strId)
    with ValT


  override type T = Val

  protected[this] sealed trait NoParent extends ValT { that: T =>
    override def parent: Option[T] = None
  }

  /** Реализация Val без подтипов. */
  protected[this] sealed class ValNoSub(strId: String) extends Val(strId) with NoParent {
    override def children: List[T] = Nil
  }

  // Элементы дерева типов N2-узлов.

  /** Юзер. */
  val Person: T   = new ValNoSub("p") {
    override def guessNodeDisplayName(mnode: MNode): Option[String] = {
      mnode.meta.person
        .emails.headOption
        .orElse {
          import mnode.meta.person._
          if (nameFirst.nonEmpty || nameLast.nonEmpty) {
            val nameFull = nameFirst.fold("")(_ + " ") + nameLast.getOrElse("")
            Some(nameFull)
          } else {
            None
          }
        }
        .orElse {
          super.guessNodeDisplayName(mnode)
        }
    }
  }

  /** Узел ADN. */
  val AdnNode: T  = new ValNoSub("n") {
    override def publicCanReadInfoAboutAdvOn = true
  }

  /** Рекламная карточка. */
  val Ad: T       = new ValNoSub("a")

  /** Теги/ключевые слова. */
  val Tag: T      = new ValNoSub("t") {
    override def guessNodeDisplayName(mnode: MNode): Option[String] = {
      mnode.edges
        .iterator
        .flatMap(_.info.tags)
        .toStream
        .headOption
        .orElse { super.guessNodeDisplayName(mnode) }
    }
  }

  /** Картинки, видео и т.д. */
  val Media       = new Val("m") with NoParent { that =>

    private trait _Parent extends ValNoSub {
      override def parent: Option[T] = Some(that)
    }

    /** Загруженная картинка. */
    val Image: T  = new ValNoSub("i") with _Parent

    // TODO Video, Audio, Document, etc...

    /** Файл, не относящийся ни к картикам, ни к видео, ни к иным категориям из Media. */
    val OtherFile: T = new ValNoSub("mf") with _Parent

    override def children = List[T](Image, OtherFile)

  }


  /** Маячок BLE. iBeacon или EddyStone -- системе это не важно. */
  val BleBeacon = new ValNoSub("b") {

    /** Маячок -- тоже узел для направленного размещения, с какими-то своими правилами. */
    override def publicCanReadInfoAboutAdvOn = true

    /**
      * Узлам-маячкам надо хранить свои uid'ы в _id. Так хоть и длинее,
      * но всё-таки нет необходимости в ведении ещё одного индекса.
      */
    override def randomIdAllowed = false

    /** Юзер управляет маячками самостоятельно. */
    override def userHasExtendedAcccess: Boolean = true

  }


  /** Поддержка binding'а из URL query string, для play router'а. */
  implicit def mNodeTypeQsb(implicit strB: QueryStringBindable[String]): QueryStringBindable[T] = {
    new QueryStringBindableImpl[T] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, T]] = {
        for (strIdEith <- strB.bind(key, params)) yield {
          strIdEith.right.flatMap { strId =>
            maybeWithName(strId)
              .fold [Either[String, T]] {
                Left("node.type.invalid")
              } { ntype =>
                Right(ntype)
              }
          }
        }
      }
      override def unbind(key: String, value: T): String = {
        strB.unbind(key, value.strId)
      }
    }
  }



  import play.api.data._, Forms._

  /** Опциональный маппинг для play-формы. */
  def mappingOptM: Mapping[Option[T]] = {
    optional( nonEmptyText(minLength = 1, maxLength = 10) )
      .transform [Option[T]] (
        _.flatMap( maybeWithName ),
        _.map(_.strId)
      )
  }

  /** Обязательный маппинг для play-формы. */
  def mappingM: Mapping[T] = {
    mappingOptM
      .verifying("error.required", _.isDefined)
      .transform [T] (EmptyUtil.getF, EmptyUtil.someF)
  }

}
