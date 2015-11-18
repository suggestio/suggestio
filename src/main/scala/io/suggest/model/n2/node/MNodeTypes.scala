package io.suggest.model.n2.node

import io.suggest.common.menum.{EnumTree, EnumMaybeWithName}
import io.suggest.model.menum.EnumJsonReadsValT
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

  }

  /** Абстрактная класс одного элемента модели. */
  protected[this] abstract class Val(override val strId: String)
    extends super.Val(strId)
    with ValT


  override type T = Val

  protected sealed trait NoParent extends ValT { that: T =>
    override def parent: Option[T] = None
  }

  /** Реализация Val без подтипов. */
  private class ValNoSub(strId: String) extends Val(strId) with NoParent {
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
  val AdnNode: T  = new ValNoSub("n")

  /** Рекламная карточка. */
  val Ad: T       = new ValNoSub("a")

  /** Теги/ключевые слова. */
  val Tag: T      = new ValNoSub("t") {
    override def guessNodeDisplayName(mnode: MNode): Option[String] = {
      mnode.extras.tag
        .flatMap(_.faces.headOption)
        .map(_._2.name)
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

    override def children = List[T](Image)

  }


  /** Поддержка binding'а из URL query string, для play router'а. */
  implicit def qsb(implicit strB: QueryStringBindable[String]): QueryStringBindable[T] = {
    new QueryStringBindable[T] {
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

}
