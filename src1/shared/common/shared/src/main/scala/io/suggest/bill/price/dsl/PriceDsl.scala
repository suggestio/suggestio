package io.suggest.bill.price.dsl

import boopickle.Default._
import io.suggest.bill.{MCurrencies, MPrice}
import io.suggest.cal.m.MCalType
import io.suggest.common.empty.{NonEmpty, OptionUtil}
import io.suggest.dt.MYmd

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.04.17 22:32
  * Description: Формирование простых цен по тарифам стало чрезвычайно сложным
  * и запутанным. Особенно ярко это проявилось на попытках детализации рассчётов:
  * ничего не понятно, запутанные повторяющиеся вычисления приводят к трудно-объяснимым
  * ошибкам.
  *
  * Было решено реализовать формирование цен на базе вложенных друг в друга классов,
  * наподобии экшенов или slick-экшенов.
  */

object IPriceDslTerm {

  implicit val iPriceDslTermPickler: Pickler[IPriceDslTerm] = {
    implicit val mPriceP = MPrice.mPricePickler
    implicit val mCalP = MCalType.mCalTypePickler
    implicit val mYmdP = MYmd.mYmdPickler
    implicit val iPriceReasonP = MPriceReason.iPriceReasonPickler

    implicit val basePickler = compositePickler[IPriceDslTerm]
    // TODO 2.12: организовать через sealed trait, в 2.12 вроде бы это уже починили.
    basePickler
      .addConcreteType[BaseTfPrice]
      .addConcreteType[Mapper]
      .addConcreteType[Sum]
  }

  // TODO Нужен v2-формат, без полиморфизма, на play-json + scalaz.Tree

}


/** Базовый интерфейс-маркер для "слов" этого DSL'я. */
sealed trait IPriceDslTerm extends NonEmpty {

  /** Посчитать итоговую цену. */
  def price: MPrice

  /** Разделить текущий терм на список термов по суммарторам, которые выкидываются.
    * Полезно для детализации рассчёта по составляющим. */
  final def splitOnSum: TraversableOnce[IPriceDslTerm] = {
    splitOnSumUntil(_ => true)
  }

  /** Управляемый сплиттинг по сумматорам.
    *
    * @param f Предикат. Вызывается для каждого терма.
    *          Отвечает на вопрос, надо ли проводить сплиттинг подэлементов.
    *          Если false, то текущий элемент будет возвращён списком as-is, и обходчик перейдёт к след.элементу.
    * @return Обычно итератор термов. Но может быть и коллекция.
    */
  def splitOnSumUntil(f: IPriceDslTerm => Boolean): TraversableOnce[IPriceDslTerm] = {
    if (f(this))
      _doSplitUntil(f)
    else
      _doNotSplitThis
  }
  protected def _doSplitUntil(f: IPriceDslTerm => Boolean): TraversableOnce[IPriceDslTerm]
  protected def _doNotSplitThis = this :: Nil


  /** Сплиттинг по сумматорам в глубину, но только до уровня item'ов. */
  def splitOnSumTillItemLevel: TraversableOnce[IPriceDslTerm] = {
    splitOnSumUntil {
      // Маппер содержит причину трансформации. Резать можно только до уровня item'а.
      case m: Mapper =>
        m.reason.exists { r =>
          // Если достигаем itemType, то дальше дробить уже нельзя.
          !r.reasonType.isItemLevel
        }
      // Остальные элементы -- просто дробить по сумматорам.
      case _ => true
    }
  }


  def children: Seq[IPriceDslTerm]

  def *(multBy: Double) = Mapper(this, Some(multBy))

  /** Рекурсивный маппинг всех цен с помощью фунции.
    *
    * @param f Маппер цены.
    * @return Обновлённый текущий класс.
    */
  def mapAllPrices(f: MPrice => MPrice): IPriceDslTerm

  def find(f: IPriceDslTerm => Boolean): Option[IPriceDslTerm] = {
    findOpt { that =>
      OptionUtil.maybe( f(that) )( that )
    }
  }

  def findOpt[T](f: IPriceDslTerm => Option[T]): Option[T] = {
    f(this)
      .orElse {
        children
          .iterator
          .flatMap(_.findOpt(f))
          .toStream
          .headOption
      }
  }

  def exists(f: IPriceDslTerm => Boolean): Boolean = {
    find(f).isDefined
  }

  def findWithReasonType(rtypes: MReasonType*): Option[Mapper] = {
    findOpt {
      case m: Mapper =>
        m.reason
          .find { reason =>
            rtypes.contains( reason.reasonType )
          }
          .map(_ => m)
      case _ => None
    }
  }

  /** Вернуть все даты. */
  def dates: Iterator[MYmd] = {
    children
      .iterator
      .flatMap(_.dates)
  }

  /** Рекурсивная фильтрация содержимого терма по датам.
    *
    * @param f Функция-предикат.
    * @return Опциональный результат с новым термом.
    *         Возможна ситуация, что None, если все даты будут отфильтрованы,
    *         и это будет обнаружено на верхнем уровне рекурсии.
    */
  def filterDates(f: Option[MYmd] => Boolean): Option[IPriceDslTerm]

  override def isEmpty = false

  def maybeMapper: Option[Mapper] = None

}


/** Базовый ценник. Используется для какой-то "отсчётной" цены.
  * На сервере, при рассчёте стоимости размещения, берётся из тарифа на текущую дату.
  */
case class BaseTfPrice(
                        override val price       : MPrice,
                        mCalType                 : Option[MCalType] = None,
                        date                     : Option[MYmd]     = None    // Option на всякий случай.
                      )
  extends IPriceDslTerm
{

  override protected def _doSplitUntil(f: (IPriceDslTerm) => Boolean): TraversableOnce[IPriceDslTerm] = {
    _doNotSplitThis
  }

  override def children = Nil
  def withPrice(price2: MPrice) = copy(price = price2)

  override def mapAllPrices(f: (MPrice) => MPrice): BaseTfPrice = {
    withPrice(
      f( price )
    )
  }

  override def dates: Iterator[MYmd] = {
    date.iterator
  }

  override def filterDates(f: (Option[MYmd]) => Boolean): Option[BaseTfPrice] = {
    OptionUtil.maybe( f(date) )(this)
  }

}


/** Маппер. Изначально был мультипликатор, но его суть расширилась.
  *
  * @param multiplifier Опциональный множитель стоимости.
  *                     Умножение amount'а нижележащей стоимости на некоторую величину.
  * @param reason Причина домножения, если есть. А по идее, она есть всегда.
  * @param underlying Суть, которая домножается.
  */
final case class Mapper(
                         underlying       : IPriceDslTerm,
                         multiplifier     : Option[Double]        = None,
                         reason           : Option[MPriceReason]  = None,
                         finalPrice       : Option[MPrice]        = None
                       )
  extends IPriceDslTerm
{

  override def price: MPrice = {
    // Нанооптимизация: не обновлять прайс, если он не изменяется. Ситуация вполне возможна, т.к. Mult используется
    // и для враппинга без фактической модификации amount'а.
    finalPrice.getOrElse {
      val p0 = underlying.price
      multiplifier
        .fold(p0)(p0.*)
    }
  }

  override protected def _doSplitUntil(f: (IPriceDslTerm) => Boolean): TraversableOnce[IPriceDslTerm] = {
    // underlying-множитель будет разбит, если содержит суммартор в своём составе.
    // На каждый разбитое слагаемое надо переналожить текущий маппер.
    for {
      term <- underlying.splitOnSumUntil(f).toIterator
    } yield {
      withUnderlying( term )
    }
  }

  override def children = underlying :: Nil

  def withUnderlying(underlying2: IPriceDslTerm) = copy(underlying = underlying2, finalPrice = None)
  def withFinalPrice(finalPrice2: Option[MPrice]) = copy(finalPrice = finalPrice2)

  override def mapAllPrices(f: (MPrice) => MPrice): Mapper = {
    // Финализируем нижележащую цену.
    val und2 = underlying.mapAllPrices(f)
    val this2 = withUnderlying(und2)
    // Финализируем цену в текущем маппере.
    this2.withFinalPrice(
      Some( f(this2.price) )
    )
  }

  override def maybeMapper = Some(this)

  override def filterDates(f: (Option[MYmd]) => Boolean): Option[Mapper] = {
    // Дат на этом уровне нет, пробрасываем фильтрацию на нижний уровень.
    for (und2 <- underlying.filterDates(f)) yield {
      withUnderlying(und2)
    }
  }

}


/** Сумматор.
  * Все нижележащие цены суммируются.
  * Полезно для объединения дней.
  */
case class Sum(
                override val children : Seq[IPriceDslTerm],
                finalPrice            : Option[MPrice] = None
              )
  extends IPriceDslTerm
{

  /** Sum в теории может быть пустым. */
  override def isEmpty = children.isEmpty

  override def price: MPrice = {
    finalPrice.getOrElse {
      if (isEmpty) {
        MPrice(0d, MCurrencies.default)   // TODO Что с валютой делать?

      } else {

        val mcurr = children.head.price.currency
        val amountSum = children
          .iterator
          .map { term =>
            val p = term.price
            if (p.currency == mcurr) {
              p.amount
            } else {
              // Нельзя суммировать разные валюты. По идее, в рамках этого метода такого и не бывает.
              throw new IllegalStateException(p.currency + "!=" + mcurr)
            }
          }
          .sum

        MPrice(amountSum, mcurr)
      }
    }
  }

  override protected def _doSplitUntil(f: (IPriceDslTerm) => Boolean): TraversableOnce[IPriceDslTerm] = {
    // Суммартор разбиваем, его куски возможно тоже содержат сумматоры: их тоже дробим.
    children
      .iterator
      .flatMap( _.splitOnSumUntil(f) )
  }


  def withChildren(children2: Seq[IPriceDslTerm]) = copy(children = children2, finalPrice = None)
  def withFinalPrice(finalPrice2: Option[MPrice]) = copy(finalPrice = finalPrice2)

  override def mapAllPrices(f: (MPrice) => MPrice): Sum = {
    // Финализируем цены в подчинённых элементах.
    val children2 = for (c <- children) yield {
      c.mapAllPrices(f)
    }
    val this2 = withChildren(children2)
    // Финализируем цену обновлённого инстанса.

    this2.withFinalPrice(
      Some( f( this2.price ) )
    )
  }

  override def filterDates(f: (Option[MYmd]) => Boolean): Option[Sum] = {
    // Фильтрануть дочерние элементы.
    val chs2 = children
      .flatMap( _.filterDates(f) )
    // Если дочерних элементов не осталось после фильтрации, то вернуть None.
    OptionUtil.maybe( chs2.nonEmpty ) {
      withChildren(chs2)
    }
  }

}
