package util.blocks

import io.suggest.common.menum.EnumValue2Val
import io.suggest.model.n2.node.MNode
import io.suggest.model.play.qsb.QueryStringBindableImpl
import io.suggest.util.logs.MacroLogsImpl
import models.blk
import models.blk.ed.BimKey_t
import models.mctx.Context
import play.api.mvc.QueryStringBindable
import play.twirl.api.{Html, Template2}
import util.FormUtil.IdEnumFormMappings
import views.html.blocks._

// TODO Надо выпилить эту модель, т.к. динамический редактор с кучей карточек и переключаемыми блоками ушел в небытие.

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.04.14 16:50
 * Description: Конфиги для блоков верстки.
 *
 * 2014.may.20: По ряду различных причин, конфиги блоков были отрефакторены так, чтобы работать в режиме конструктора,
 * собираемого компилятором. Все поля -- это трейты (аддоны), которые можно добавить в блок через with/extends,
 * и компилятор разрулит автоматом все генераторы форм, маппинги и т.д.
 *
 * В связи со вспывшими подводными камнями, не удалось полностью избавится от ненужного кода, хотя оставшийся ненужный
 * код можно выправлять путём копипаста из другого блока. Конфиг блока состоит из нескольких частей:
 * - Базовый трейт. Обычно называется BlockNt (например Block19t, Block1t). Он содержит всё описание блока (все with).
 *   Разработка трейта такова:
 *     Смотрим на другой блок, делаем sealed "trait Block222t extends ... with ... with ... {}" по аналогии.
 *     Порядок полей в extends ... with ... with ... определяет порядок полей в форме.
 *     Разница в ролях ключевых слов "extends" и "with" в данном случае нивелируется, нужно просто помнить, что нужно
 *     первое поле трейта присоединяется через "extends X", а все последующие трейты полей -- через "with X".
 *     Например "trait Block222 extends Height with Title with Descr".
 *     Затем, если необходимо указать значения для конкретный полей блока (например defaultValue), внутри блока
 *     нажимаем alt+insert -> override -> выбираем нужное поле (например heightBf). После сгенеренного кода с
 *     super.heightBf добавляем .copy() и в скобках переопределяем значения полей. В итоге получается что-то типа:
 *     override def heightBf = super.heightBf.copy(
 *       defaultValue = Some(300),
 *       availableValse = Set(160, 300)
 *     )
 *
 * - val BlockN - неизменяемый инстанс элемента scala.Enumeration.Value и реализация базового трейта.
 *   Содержит в себе реализацию метода mappingWithNewKey(String), который генерит враппер над этим блоком для нужд\
 *   play mapping'ов.
 *   Разработка val Block: пишем что-то типа:
 *     val Block222 = new Val(222) with Block222t with EmptyKey {
 *       Внутрь копируем код метода mappingWithNewKey() из соседнего блока, заменив номер блока-враппера.
 *     }
 *
 * - BlockNWrapper (имеет имя Block1Wrapper, Block2Wrapper, итд). Вынужденный костыль, который отвязан от
 *   scala.Enumeration, но в остальном повторяет val BlockN. Является case-подклассом [[ValTWrapper]] и трейта BlockNt.
 *   Содержит в себе реализацию метода mappingWithNewKey(String), который вызывает copy() чтобы выставить врапперу новый
 *   mapping-ключ.
 *   Разработка враппера: копируем код враппера соседнего блока, заменив нужные числа на текущий номер блока.
 */

object BlocksConf
  extends Enumeration
  with MacroLogsImpl
  with EnumValue2Val
  with IdEnumFormMappings
{

  /** Всё описание блока идёт через наследование Val и её интерфейса [[ValT]] при необходимости. */
  protected abstract class Val(id: Int)
    extends super.Val(id)
    with ValTEmpty
    with CommonBlock2T
  {

    /**
     * label'ы опций конфига блока, прописанные в conf/messages*.
     * @param bk исходный BK_-идентификатор
     * @return идентификатор, пригодный для резолва через Messages().
      */
    override def i18nLabelOf(bk: String) = "blocks.field." + bk

  }

  override type T = Val


  /** Дефолтовый блок, если возникают сомнения. */
  def DEFAULT: T = Block20

  /**
   * Аналог apply, но вызывает DEFAULT(), если нет блока с необходимым id.
   * @param n id искомого блока.
   * @return Экземпляр BlocksConf.
   */
  def applyOrDefault(n: Int): T = {
    try {
      apply(n)
    } catch {
      case _: NoSuchElementException =>
        DEFAULT
    }
  }
  def applyOrDefault(nOpt: Option[Int]): T = {
    nOpt.fold(DEFAULT)(applyOrDefault)
  }
  def applyOrDefault(mad: MNode): T = {
    DEFAULT
  }

  // Начало значений

  /** Блок рекламной карточки с произвольным заполнением и без svg. */
  sealed trait CommonBlock2T extends ValT

  /** Блок рекламной карточки с произвольным заполнением и без svg. */
  sealed trait Block20t extends CommonBlock2T {
    override def ordering = 1000
    override def template = _block20Tpl
  }
  val Block20 = new Val( 20 ) with Block20t with EmptyKey {
  }
  sealed case class Block20Wrapper(key: String) extends ValTWrapper(Block20) with ValTEmpty with Block20t


  // Конец значений. Уже. А ведь когда-то их было 26...


  /** Отображаемые блоки. Обращение напрямую к values порождает множество с неопределённым порядком,
    * а тут - сразу отсортировано по id и только отображаемые. */
  val valuesShown: Seq[T] = {
    val vs0 = values
      .asInstanceOf[ collection.Set[T] ]
      .toSeq
      .filter(_.isShown)
    orderBlocks(vs0)
  }

  /** Отсортировать блоки согласно ordering с учётом id. */
  def orderBlocks(values: Seq[T]) = {
    values.sortBy { bc => bc.ordering -> bc.id }
  }

  /** Поддержка биндинга блока из routes. */
  implicit def blocksConfQsb(implicit intB: QueryStringBindable[Int]): QueryStringBindable[T] = {
    new QueryStringBindableImpl[T] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, T]] = {
        for {
          blockIdEith <- intB.bind(key, params)
        } yield {
          for {
            blockId <- blockIdEith.right
          } yield {
            applyOrDefault(blockId)
          }
        }
      }

      override def unbind(key: String, value: T): String = {
        intB.unbind(key, value.id)
      }
    }
  }

}



/** Базовый интерфейс для реализаций класса Enumeration.Val. */
trait ValT {

  def id: Int

  def ordering: Int = 10000

  def isShown = true

  /** Флаг того, что на блок не стоит навешивать скрипты, отрабатывающие клик по нему. */
  def hrefBlock = false

  /** Шаблон для рендера. */
  def template: Template2[blk.IRenderArgs, Context, Html]

  /** Более удобный интерфейс для метода template.render(). */
  def renderBlock(args: blk.IRenderArgs)(implicit ctx: Context) = {
    template.render(args, ctx)
  }

  /**
   * label'ы опций конфига блока, прописанные в conf/messages*.
   * @param bk исходный BK_-идентификатор
   * @return идентификатор, пригодный для резолва через Messages().
   */
  def i18nLabelOf(bk: String): String

  /** Поиск поля картинки для указанного имени поля. */
  def getImgFieldForName(fn: BimKey_t): Option[BfImage] = None

}



abstract class ValTWrapper(v: ValT) extends ValT {
  override def id = v.id
  override def i18nLabelOf(bk: String) = {
    v.i18nLabelOf(bk)
  }
}


/** Враппер понадобился из-за проблем со scala.Enumeration, который не даёт делать инстансы Val несколько раз. */
trait ValTEmpty extends ValT


trait EmptyKey extends ValT

