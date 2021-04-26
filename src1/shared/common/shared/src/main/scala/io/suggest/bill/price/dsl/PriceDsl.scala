package io.suggest.bill.price.dsl

import enumeratum.values._
import io.suggest.bill.MPrice
import io.suggest.cal.m.MCalType
import io.suggest.dt.MYmd
import io.suggest.enum2.EnumeratumUtil
import io.suggest.scalaz.ScalazUtil.Implicits._
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._
import monocle.macros.GenLens
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scalaz.{EphemeralStream, Equal, Tree}

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
object PriceDsl {

  @inline implicit def univEq: UnivEq[PriceDsl] = UnivEq.derive

  object Fields {
    final def TERM = "t"
    final def PRICE = "p"
    final def CAL_TYPE = "c"
    final def DATE = "d"
    final def MULT = "m"
    final def REASON = "r"
  }

  implicit def priceDslJson: OFormat[PriceDsl] = {
    val F = Fields
    (
      (__ \ F.TERM).format[PriceDslTerm] and
      (__ \ F.PRICE).formatNullable[MPrice] and
      (__ \ F.CAL_TYPE).formatNullable[MCalType] and
      (__ \ F.DATE).formatNullable[MYmd] and
      (__ \ F.MULT).formatNullable[Double] and
      (__ \ F.REASON).formatNullable[MPriceReason]
    )(apply, unlift(unapply))
  }

  def price = GenLens[PriceDsl](_.price)

  def base(
            price             : MPrice,
            mCalType          : Option[MCalType]      = None,
            date              : Option[MYmd]          = None,    // Option на всякий случай.
          ): PriceDsl = {
    apply(
      term      = PriceDslTerms.BasePrice,
      price     = Some( price ),
      mCalType  = mCalType,
      date      = date,
    )
  }


  def mapper(
              multiplifier     : Option[Double]        = None,
              reason           : Option[MPriceReason]  = None,
              price            : Option[MPrice]        = None
            ): PriceDsl = {
    apply(
      term          = PriceDslTerms.Mapper,
      multiplifier  = multiplifier,
      reason        = reason,
      price         = price,
    )
  }


  def sum(
           price                : Option[MPrice]       = None,
         ): PriceDsl = {
    apply(
      term        = PriceDslTerms.Sum,
      price       = price,
    )
  }


  implicit final class PriceDslTreeExt( private val priceDslTree: Tree[PriceDsl] ) extends AnyVal {

    def _sumChildren = priceDslTree
      .subForest
      .map(_.price)
      .iterator
      .reduce { (p1, p2) =>
        if (p1.currency ==* p2.currency) {
          MPrice(
            amount   = p1.amount + p2.amount,
            currency = p1.currency,
          )
        } else {
          throw new IllegalArgumentException( p1.currency.toString + "!=" + p2.currency.toString )
        }
      }


    /** Посчитать итоговую цену. */
    def price: MPrice = {
      val priceDsl = priceDslTree.rootLabel
      priceDsl
        .price
        .getOrElse[MPrice] {
          priceDsl.term match {
            case PriceDslTerms.Mapper =>
              val p0 = _sumChildren
              priceDsl.multiplifier
                .fold(p0)(p0.*)
            case PriceDslTerms.Sum =>
              _sumChildren
            case PriceDslTerms.BasePrice =>
              throw new IllegalStateException
          }
        }
    }

    /** Разделить текущий терм на список термов по суммарторам, которые выкидываются.
      * Полезно для детализации рассчёта по составляющим. */
    def splitOnSum: EphemeralStream[Tree[PriceDsl]] =
      splitOnSumUntil(_ => true)

    /** Управляемый сплиттинг.
      *
      * @param f Предикат. Вызывается для каждого терма.
      *          Отвечает на вопрос, надо ли проводить сплиттинг подэлементов.
      *          Если false, то текущий элемент будет возвращён списком as-is, и обходчик перейдёт к след.элементу.
      * @return Обычно итератор термов. Но может быть и коллекция.
      */
    def splitOnSumUntil(f: PriceDsl => Boolean): EphemeralStream[Tree[PriceDsl]] = {
      val priceDsl = priceDslTree.rootLabel

      if (f(priceDsl)) {
        priceDsl.term match {
          case PriceDslTerms.Mapper =>
            // Множитель будет разбит, если содержит суммартор в своём составе.
            // На каждый разбитое слагаемое надо переналожить текущий маппер.
            for {
              term <- priceDslTree
                .subForest
                .flatMap( _.splitOnSumUntil(f) )
            } yield {
              Tree.Node(
                priceDsl,
                EphemeralStream( term ),
              )
            }

          // Суммартор разбиваем, его куски возможно тоже содержат сумматоры: их тоже дробим.
          case PriceDslTerms.Sum =>
            priceDslTree
              .subForest
              .flatMap( _.splitOnSumUntil(f) )

          case PriceDslTerms.BasePrice =>
            _doNotSplitThis
        }
      } else {
        _doNotSplitThis
      }
    }

    private def _doNotSplitThis = EphemeralStream( priceDslTree )


    /** Сплиттинг по сумматорам в глубину, но только до уровня item'ов. */
    def splitOnSumTillItemLevel: EphemeralStream[Tree[PriceDsl]] = {
      splitOnSumUntil { priceDsl =>
        priceDsl.term match {
          // Маппер содержит причину трансформации. Резать можно только до уровня item'а.
          case PriceDslTerms.Mapper =>
            priceDsl.reason.exists { r =>
              // Если достигаем itemType, то дальше дробить уже нельзя.
              !r.reasonType.isItemLevel
            }
          // Остальные элементы -- просто дробить по сумматорам.
          case _ => true
        }
      }
    }

    def *(multBy: Double): Tree[PriceDsl] = {
      Tree.Node(
        PriceDsl.mapper(
          multiplifier = Some( multBy ),
        ),
        _doNotSplitThis,
      )
    }

    def ensureFinalPrice: Tree[PriceDsl] = {
      val priceDsl = priceDslTree.rootLabel
      priceDsl
        .price
        .fold {
          Tree.Node(
            (PriceDsl.price set Some( priceDslTree.price ))(priceDsl),
            priceDslTree.subForest
          )
        }( _ => priceDslTree )
    }

    /** Рекурсивный маппинг всех цен с помощью фунции.
      *
      * @param f Маппер цены.
      * @return Обновлённый текущий класс.
      */
    def mapAllPrices(f: MPrice => MPrice): Tree[PriceDsl] = {
      val priceDsl = priceDslTree.rootLabel
      priceDsl.term match {
        case PriceDslTerms.Mapper | PriceDslTerms.Sum =>
          Tree.Node(
            priceDsl,
            priceDslTree
              .subForest
              .map(_.mapAllPrices(f)),
          )
            // Финализируем цену в текущем маппере.
            .ensureFinalPrice

        case PriceDslTerms.BasePrice =>
          priceDsl
            .price
            .fold(priceDslTree) { price0 =>
              Tree.Node(
                (PriceDsl.price set Some(f(price0)))(priceDsl),
                priceDslTree.subForest
              )
            }
      }
    }


    def findOpt[T](f: Tree[PriceDsl] => Option[T]): Option[T] = {
      priceDslTree
        .cobind(identity)
        .flatten
        .iterator
        .flatMap(f)
        .nextOption()
    }


    def findWithReasonType(rtypes: MReasonType*): Option[Tree[PriceDsl]] = {
      findOpt { priceDslSubtree =>
        priceDslSubtree
          .rootLabel
          .reason
          .find { reason =>
            rtypes contains[MReasonType] reason.reasonType
          }
          .map(_ => priceDslSubtree)
      }
    }


    /** Вернуть все даты. */
    def dates: Iterator[MYmd] = {
      val priceDsl = priceDslTree.rootLabel
      priceDsl.term match {
        case PriceDslTerms.BasePrice =>
          priceDsl
            .date
            .iterator

        case _ =>
          priceDslTree
            .subForest
            .iterator
            .flatMap(_.dates)
      }
    }


    /** Рекурсивная фильтрация содержимого терма по датам.
      *
      * @param f Функция-предикат.
      * @return Опциональный результат с новым термом.
      *         Возможна ситуация, что None, если все даты будут отфильтрованы,
      *         и это будет обнаружено на верхнем уровне рекурсии.
      */
    def filterDates(f: Option[MYmd] => Boolean): Option[Tree[PriceDsl]] = {
      val priceDsl = priceDslTree.rootLabel
      priceDsl.term match {
        case PriceDslTerms.BasePrice =>
          Option.when( f(priceDsl.date) )(priceDslTree)

        case _ =>
          // Фильтрануть дочерние элементы.
          val eph0 = EphemeralStream.emptyEphemeralStream[Tree[PriceDsl]]
          val subForest2 = priceDslTree
            .subForest
            .flatMap { r =>
              r .filterDates( f )
                .fold( eph0 )(_ ##:: eph0)
            }

          // Если дочерних элементов не осталось после фильтрации, то вернуть None.
          Option.when( !subForest2.isEmpty ) {
            Tree.Node( priceDsl, subForest2 )
          }
      }
    }


    def isEmpty: Boolean = {
      val priceDsl = priceDslTree.rootLabel
      priceDsl.term match {
        case PriceDslTerms.BasePrice =>
          false

        case _ =>
          priceDslTree
            .subForest
            .isEmpty
      }
    }

  }

  implicit def priceDslEqual: Equal[PriceDsl] =
    Equal.equalA

}


/** Единый класс-контейнер для каждого элемента PriceDsl.
  *
  * @param term Тип терма PriceDsl.
  * @param price Для BasePrice тут обязательный обязательный базовый ценник.
  *              Используется для какой-то "отсчётной" цены.
  *              Для Sum/Mapper здесь подразумевается finalPrice - посчитанная цена дочерних элементов.
  * @param mCalType На сервере, при рассчёте стоимости размещения, берётся из тарифа на текущую дату.
  * @param date Текущиая дата.
  * @param multiplifier Мультипликатор дочерних элементов.
  * @param reason Причина маппинга значений.
  */
final case class PriceDsl(
                           term              : PriceDslTerm,
                           price             : Option[MPrice]        = None,
                           // term = BasePrice
                           mCalType          : Option[MCalType]      = None,
                           date              : Option[MYmd]          = None,    // Option на всякий случай.
                           // term = Mapper
                           multiplifier      : Option[Double]        = None,
                           reason            : Option[MPriceReason]  = None,
                         )



sealed abstract class PriceDslTerm(override val value: String) extends StringEnumEntry

object PriceDslTerm {
  @inline implicit def univEq: UnivEq[PriceDslTerm] = UnivEq.derive
  implicit def priceDslTermJson: Format[PriceDslTerm] =
    EnumeratumUtil.valueEnumEntryFormat( PriceDslTerms )
}

object PriceDslTerms extends StringEnum[PriceDslTerm] {
  case object BasePrice extends PriceDslTerm("p")
  case object Mapper extends PriceDslTerm("m")
  case object Sum extends PriceDslTerm("s")
  override def values = findValues
}
