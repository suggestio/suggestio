package io.suggest.lk.adv.geo.r.bill

import diode.react.ModelProxy
import io.suggest.bill.price.dsl.{BaseTfPrice, IPriceDslTerm, Mapper, Sum}
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.sjs.common.i18n.{JsFormatUtil, Messages}
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB, ReactElement, ReactNode}
import io.suggest.i18n.MsgCodes
import io.suggest.react.ReactCommonUtil.Implicits.reactElOpt2reactEl
import io.suggest.lk.r.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.react.r.YmdR

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.04.17 18:23
  * Description: React-компонент с таблицей текущих псевдо-item'ов и цен по ним.
  */
object ItemsPricesR {

  type Props = ModelProxy[Option[IPriceDslTerm]]


  class Backend($: BackendScope[Props, Unit]) {

    private val tdCssBase = Css.Table.Td.TD :: Css.Table.Td.WHITE :: Css.Size.M :: Nil
    private val tdCssHead = Css.Table.Td.GRAY :: tdCssBase
    private val tdCssBody = Css.Table.Td.WHITE :: tdCssBase

    /** Рендер строки маппера с данными его полей (кроме underlying). */
    def _renderMapperReason(mapper: Mapper) = {
      <.tr(
        for (priceReason <- mapper.reason) yield {
          // Рендерить название причины начисления
          val leftTd = <.td(
            ^.`class` := Css.flat1( Css.Table.Td.Radial.FIRST :: tdCssBody ),

            Messages( priceReason.reasonType.msgCodeI18n )
          )

          // Рендерить переменные, присланные вместе с причиной.
          val secondTd = <.td(
            ^.`class` := Css.flat1( tdCssBody ),

            priceReason.reasonType
              .i18nPayload(priceReason)( Messages.f )
              .getOrElse[String]( HtmlConstants.NBSP_STR )
          )

          // Рендерить множитель причины.
          val thirdTd = <.td(
            ^.`class` := Css.flat1( tdCssBody ),
            ^.width := 40.px,

            mapper
              .multiplifier
              .fold[ReactNode]( HtmlConstants.NBSP_STR ) { mult =>
              <.span(
                "x", "%1.2f".format(mult)
              )
            }

          )

          leftTd ::
            secondTd ::
            thirdTd ::
            Nil
        },

        // Рендерить цену после домножения на множитель
        <.td(
          ^.`class` := Css.flat1( Css.Table.Td.Radial.LAST :: tdCssBody ),
          ^.width   := 100.px,
          JsFormatUtil.formatPrice( mapper.price )
        )

      )
    }


    /** Рендер одного ряда с под-элементами. */
    private def _renderUnderlyingRow(und: IPriceDslTerm, undLevel: Int, withTableOuter: Boolean = true) = {
      <.tr(
        <.td(
          ^.`class` := Css.flat1( tdCssBody ),
          ^.colSpan := 4,
          _renderPriceTerm(und, level = undLevel, withTableOuter = withTableOuter)
        )
      )
    }


    /** Рендер данных по тарифу. */
    private def _renderBaseTfPriceData(tfPrice: BaseTfPrice) = {
      // Желательно не более трёх полей, т.к. это дело на самом нижнем уровне рендерится с ощутимым сдвигом слева.
      <.tr(
        // Дата
        <.td(
          ^.`class` := Css.flat1( Css.Table.Td.Radial.FIRST :: tdCssBody ),

          tfPrice.date
            .fold[ReactNode]( HtmlConstants.NBSP_STR ) { ymd =>
              YmdR( ymd )(
                HtmlConstants.COMMA,
                HtmlConstants.SPACE,
                JsFormatUtil.formatDow( ymd )
              )
            }
        ),

        // Рендерить название типа использованного календаря.
        <.td(
          ^.`class` := Css.flat1( tdCssBody ),
          tfPrice.mCalType
            .fold[ReactNode]( HtmlConstants.NBSP_STR ) { calType =>
              Messages( calType.name )
            }
        ),

        // Рендерить цены по тарифу для календаря.
        <.td(
          ^.`class` := Css.flat1( Css.Table.Td.Radial.LAST :: tdCssBody ),
          ^.width   := 100.px,
          JsFormatUtil.formatPrice( tfPrice.price ),
          Messages( MsgCodes.`_per_.day` )
        )
      )
    }


    private def _renderChildren(term: IPriceDslTerm, sumLevel: Int) = {
      val undLevel = sumLevel + 1
      for {
        (c, i) <- term.children.iterator.zipWithIndex
      } yield {
        _renderUnderlyingRow(c, undLevel = undLevel)(
          ^.key := i.toString
        )
      }
    }


    private def _renderOuterTableForRow(term: IPriceDslTerm, level: Int) = {
      val thead: TagMod = term match {
        // На основе маппера нужно собрать заголовок на 4 колонки.
        case _: Mapper =>
          EmptyTag
          /*
          mapper.reason.fold[TagMod](EmptyTag) { _ =>
            <.thead(
              <.tr(
                <.td(
                  ^.`class` := Css.flat1( Css.Table.Td.Radial.FIRST :: tdCssHead ),
                  "#1"
                ),

                <.td(
                  ^.`class` := Css.flat1( tdCssHead ),
                  "#2"
                ),

                <.td(
                  ^.`class` := Css.flat1( tdCssHead ),
                  "#3"
                ),

                <.td(
                  ^.`class` := Css.flat1( Css.Table.Td.Radial.LAST :: tdCssHead ),
                  Messages( MsgCodes.`Price` )
                )
              )
            )
          }
          */

        case tfPrice: BaseTfPrice =>
          <.thead(
            <.tr(
              // Колонка даты, если есть.
              for (_ <- tfPrice.date) yield {
                <.td(
                  ^.`class` := Css.flat1(Css.Table.Td.Radial.FIRST :: tdCssHead),
                  Messages( MsgCodes.`Date` )
                ): ReactElement
              },

              // Колонка календаря
              for (_ <- tfPrice.mCalType) yield {
                <.td(
                  ^.`class` := Css.flat1( tdCssHead ),
                  HtmlConstants.NBSP_STR
                ): ReactElement
              },

              // Колонка цены.
              <.td(
                ^.`class` := Css.flat1(Css.Table.Td.Radial.LAST :: tdCssHead),
                ^.width := 40.px,
                Messages( MsgCodes.`Price` )
              )
            )
          )

        case _: Sum =>
          // Заголовок не нужен.
          EmptyTag

      }

      <.table(
        ^.`class` := Css.flat( Css.Table.TABLE, Css.Table.Width.XL ),
        ^.marginLeft := (level * 2).px,
        thead
      )
    }


    private def _renderPriceTerm(term: IPriceDslTerm, level: Int = 0, withTableOuter: Boolean = true): ReactNode = {
      val contentRows: Seq[ReactElement] = term match {
        // У нас тут маппер. Рендерить его и его содержимое.
        case mapper: Mapper =>
          Seq(
            _renderMapperReason(mapper)(
              ^.key := "r"
            ),
            _renderUnderlyingRow( mapper.underlying, undLevel = level + 1, withTableOuter = true )(
              ^.key := "u"
            )
          )

        case tfPrice: BaseTfPrice =>
          Seq(
            _renderBaseTfPriceData( tfPrice )(
              ^.key := "a"
            )
          )

        case sum: Sum =>
          _renderChildren(sum, level)
            .toSeq
      }

      if (withTableOuter) {
        term.children
          .headOption
          .fold[ReactNode](contentRows) { firstChild =>
            _renderOuterTableForRow(firstChild, level)(
              <.tbody(
                contentRows
              )
            )
          }
      } else {
        contentRows
      }
    }


    def render(proxy: Props): ReactElement = {
      println(proxy())
      for (term <- proxy()) yield {
        <.div(
          _renderPriceTerm(term)
        ): ReactElement
      }
    }

  }


  val component = ReactComponentB[Props]("ItemsPrices")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(priceDslOptProxy: Props) = component( priceDslOptProxy )

}
