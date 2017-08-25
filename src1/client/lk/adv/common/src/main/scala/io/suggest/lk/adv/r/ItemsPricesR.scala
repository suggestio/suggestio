package io.suggest.lk.adv.r

import diode.react.ModelProxy
import io.suggest.bill.price.dsl.{BaseTfPrice, IPriceDslTerm, Mapper, Sum}
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.r.YmdR
import io.suggest.sjs.common.i18n.{JsFormatUtil, Messages}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, ScalaComponent}

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
    private val tdCssBody = Css.Table.Td.WHITE :: tdCssBase

    /** Рендер строки маппера с данными его полей (кроме underlying). */
    def _renderMapperReason(mapper: Mapper) = {
      <.tr(
        mapper.reason.whenDefined { priceReason =>
          // Рендерить название причины начисления
          val leftTd = <.td(
            ^.`class` := Css.flat1( tdCssBody ),

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
              .fold[VdomNode]( HtmlConstants.NBSP_STR ) { mult =>
                val absMult = Math.abs(mult)

                var fracDigits = 2
                if (absMult < 0.02)
                  fracDigits += 1
                //if (absMult < 0.002)
                //  fracDigits += 1
                val fmt = "%1." + fracDigits + "f"

                <.span(
                  "x", fmt.format(mult)
                )
              }

          )

          VdomArray(
            leftTd,
            secondTd,
            thirdTd
          )
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
          ^.`class` := Css.flat1( tdCssBody ),

          tfPrice.date
            .fold[VdomNode]( HtmlConstants.NBSP_STR ) { ymd =>
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
            .fold[VdomNode]( HtmlConstants.NBSP_STR ) { calType =>
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
      term.children.toVdomArray { c =>
        _renderPriceTerm(c, level = undLevel, withTableOuter = false)
      }
    }


    private def _renderOuterTableForRow(term: IPriceDslTerm, level: Int) = {
      <.table(
        ^.`class` := Css.flat( Css.Table.TABLE, Css.Table.Width.XL ),
        ^.marginLeft := (level * 2).px
      )
    }


    private def _renderPriceTerm(term: IPriceDslTerm, level: Int = 0, withTableOuter: Boolean = true): VdomNode = {
      val contentRows: VdomNode = term match {
        // У нас тут маппер. Рендерить его и его содержимое.
        case mapper: Mapper =>
          VdomArray(
            _renderMapperReason(mapper)(
              ^.key := "r"
            ),
            _renderUnderlyingRow( mapper.underlying, undLevel = level + 1, withTableOuter = true )(
              ^.key := "u"
            )
          )

        case tfPrice: BaseTfPrice =>
          VdomArray(
            _renderBaseTfPriceData( tfPrice )(
              ^.key := "a"
            )
          )

        case sum: Sum =>
          _renderChildren(sum, level)
      }

      if (withTableOuter) {
        term.children
          .headOption
          .fold[VdomNode](contentRows) { firstChild =>
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


    def render(proxy: Props): VdomElement = {
      proxy().whenDefinedEl { term =>
        <.div(
          _renderPriceTerm(term)
        )
      }
    }

  }


  val component = ScalaComponent.builder[Props]("ItemsPrices")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(priceDslOptProxy: Props) = component( priceDslOptProxy )

}
