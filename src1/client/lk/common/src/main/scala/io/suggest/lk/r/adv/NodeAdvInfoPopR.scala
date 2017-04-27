package io.suggest.lk.r.adv

import diode.react.ModelProxy
import io.suggest.lk.m.NodeInfoPopupClose
import io.suggest.lk.pop.PopupR
import japgolly.scalajs.react.{BackendScope, Callback, ReactComponentB, ReactElement, ReactNode}
import japgolly.scalajs.react.vdom.prefix_<^._
import io.suggest.react.ReactCommonUtil.Implicits.reactElOpt2reactEl
import io.suggest.lk.r.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.adv.info.MNodeAdvInfo
import io.suggest.bill.MPrice
import io.suggest.cal.m.MCalType
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import io.suggest.model.n2.node.meta.MMetaPub
import io.suggest.sjs.common.controller.DomQuick
import io.suggest.sjs.common.i18n.{JsFormatUtil, Messages}
import react.image.gallery.{IgItem, ImageGalleryPropsR, ImageGalleryR}
import io.suggest.sjs.common.empty.JsOptionUtil.opt2undef

import scala.scalajs.js.JSConverters._
import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.03.17 13:17
  * Description: React-компонент для рендера ответа сервера на тему инфы по размещению на узле.
  */
object NodeAdvInfoPopR {

  // Не Pot[], т.к. рендер ошибок и ожиданий -- отдельно на уровне формы.
  type Props = ModelProxy[Option[MNodeAdvInfo]]


  /** Модуль, управляющий рендером компонента. */
  class Backend($: BackendScope[Props, Unit]) {

    def popupCloseClick: Callback = {
      dispatchOnProxyScopeCB( $, NodeInfoPopupClose )
    }


    /** Ряд заголовка. */
    private def _tableHeaderRow(clauses: Iterable[(MCalType, MPrice)]): ReactElement = {
      val firstCalTypeOpt = clauses.headOption.map(_._1)

      <.tr(
        <.td(
          ^.colSpan := 2,
          HtmlConstants.NBSP_STR
        ),

        clauses.flatMap { case (mCalType, _) =>
          // Ячейка заголовка с описанием названия (типа) календаря и затрагиваемых дней недели.
          val payloadCell = <.td(
            ^.key := mCalType.strId,
            ^.`class` := Css.flat( Css.Table.Td.TD, Css.Lk.Adv.NodeInfo.TARIFF_INFO_TITLE ),
            Messages( mCalType.name ),
            ": ",
            for (dayStart <- mCalType.dayStart) yield {
              Messages( MsgCodes.`dayOfW.N.`( dayStart ) )
            },
            (mCalType.dayStart.nonEmpty && mCalType.dayEnd.nonEmpty) ?= "-",
            for (dayEnd <- mCalType.dayEnd) yield {
              Messages( MsgCodes.`dayOfW.N.`( dayEnd ) )
            }
          )
          val acc0 = payloadCell :: Nil

          // Из-за "особенностей" верстки макса требуется добавлять пустые ячейки между столбцами.
          if ( !firstCalTypeOpt.contains( mCalType ) ) {
            val delimTd = <.td(
              ^.key := mCalType.strId + "-"
            )
            delimTd :: acc0
          } else {
            acc0
          }
        }

      )
    }


    /** Ячейки ряда с ценами. */
    private def _pricesRow(rowTitle1: String, rowTitle2: String, clauses: Iterable[(MCalType, MPrice)]): ReactElement = {
      val sutki = Messages( MsgCodes.`day24h` )
      val td = Css.Table.Td.TD
      <.tr(
        // Левая колонка: описание принадлежности перечисленных цен.
        <.td(
          ^.`class` := Css.flat( Css.Table.Td.TD, Css.Lk.Adv.NodeInfo.TARIFF_GREEN, Css.Font.Sz.XS, Css.Colors.WHITE ),
          rowTitle1,
          <.br,
          rowTitle2
        ),

        clauses.flatMap { case (mCalType, mPrice) =>
          // td-разделитель, из-за особенностей вёрстки.
          val td1 = <.td(
            ^.key := mCalType.strId + "_",
            ^.`class` := td
          )
          // Непосредственный контент.
          val td2 = <.td(
            ^.key := mCalType.strId,
            ^.`class` := Css.flat( td, Css.Lk.Adv.NodeInfo.TARIFF_INFO_VALUE ),
            JsFormatUtil.formatPrice( mPrice ),
            "/",
            sutki
          )
          td1 :: td2 :: Nil
        }
      )
    }


    /** Рендер одного ряда в метаданных. */
    private def _renderMetaRow(msgCode: String, vOpt: Option[String], isUrl: Boolean = false): ReactElement = {
      for (v <- vOpt) yield {
        <.div(
          <.table(
            ^.`class` := Css.PropTable.TABLE,
            <.tbody(
              <.tr(
                <.td(
                  ^.`class` := Css.PropTable.TD_NAME,
                  Messages(msgCode)
                ),
                <.td(
                  ^.`class` := Css.PropTable.TD_VALUE,
                  if (isUrl) {
                    <.a(
                      ^.href := v,
                      ^.target := HtmlConstants.Target.blank,
                      v
                    )
                  } else {
                    v
                  }
                )
              )
            )
          ),
          <.hr(
            ^.`class` := Css.flat( Css.Lk.HrDelim.DELIMITER, Css.Lk.HrDelim.LIGHT )
          )
        ): ReactElement
      }
    }

    /** Рендер блока метаданных узла. */
    private def _renderMeta(mmeta: MMetaPub) = {
      <.div(
        ^.`class` := Css.Overflow.HIDDEN,
        _renderMetaRow( MsgCodes.`Town`, mmeta.address.town ),
        _renderMetaRow( MsgCodes.`Address`, mmeta.address.address ),
        _renderMetaRow( MsgCodes.`Site`, mmeta.business.siteUrl, isUrl = true ),
        _renderMetaRow( MsgCodes.`Info.about.prods.and.svcs`, mmeta.business.info ),
        _renderMetaRow( MsgCodes.`Daily.people.traffic`, mmeta.business.humanTrafficAvg.map(_.toString) ),
        _renderMetaRow( MsgCodes.`Audience.descr`, mmeta.business.audienceDescr )
      )
    }


    /** React-only рендер попапа. Исходный рендер работал через innerHtml.
      *
      * @param advInfoOptProxy Прокся props.
      * @return React element.
      */
    def render( advInfoOptProxy: Props ): ReactElement = {
      for (advInfo <- advInfoOptProxy()) yield {
        advInfoOptProxy.wrap { _ =>
          PopupR.PropsVal(
            closeable = Some( popupCloseClick ),
            topPc     = 9
          )
        } { popPropsProxy =>
          // Рендер состояния реквеста данных с сервера.
          PopupR(popPropsProxy)(

            <.div(

              // Заголовок попапа.
              <.h2(
                ^.`class` := Css.Lk.MINOR_TITLE,
                Messages( MsgCodes.`Tariff.rate.of.0`, advInfo.nodeName )
              ),

              // Контент попапа.
              <.div(
                ^.`class` := Css.flat( Css.Lk.Adv.NodeInfo.TARIFF, Css.Lk.Adv.NodeInfo.IN_POPUP ),

                // Галерея фоток, если есть.
                advInfo.gallery.nonEmpty ?= {
                  <.div(
                    ^.`class` := Css.Lk.Adv.NodeInfo.TARIFF_PHOTO_LIST,
                    // Юзаем тут react-галеру вместо всяких кривых bxSlider:
                    ImageGalleryR(
                      new ImageGalleryPropsR {
                        override val autoPlay = true
                        override val items: js.Array[IgItem] = {
                          val igItems = for {
                            mediaInfo <- advInfo.gallery.iterator
                          } yield {
                            new IgItem {
                              override val original = mediaInfo.url
                              override val thumbnail: UndefOr[String] = {
                                mediaInfo.thumb.map(_.url)
                              }
                              override val originalClass: UndefOr[String] = {
                                Css.Lk.Adv.NodeInfo.TARIFF_PHOTO_IMG
                              }
                            }
                          }
                          igItems.toJSArray
                        }
                        override val showThumbnails = false
                        override val showNav = false
                        override val showBullets = true
                        override val slideInterval = 3000
                        override val slideDuration = 450
                        override val showFullscreenButton = true
                      }
                    )()
                  )
                },

                // Левая схема-решётка про минимальный модуль карточки.
                <.div(
                  ^.`class` := Css.Lk.Adv.NodeInfo.TARIFF_ILLUSTRATION_W,
                  <.div(
                    ^.`class` := Css.Lk.Adv.NodeInfo.TARIFF_ILLUSTRATION
                  )
                ),

                /*
                 <div class="ovh"><table class="tariff_info"><tbody><tr><td colspan="2">&nbsp;</td><td class="td tariff_info-title"> Вся неделя: пн-вс </td><td></td><td class="td tariff_info-title"> Выходные дни: пт-вс </td><td></td><td class="td tariff_info-title"> Праздники/праймтайм: </td></tr><tr><td class="td tariff_green ft-XS white">Минимальный&nbsp;модуль <br>(на&nbsp;схеме&nbsp;слева) </td><td class="td"></td><td class="td tariff_info-value">2&nbsp;р./сутки </td><td class="td"></td><td class="td tariff_info-value">4&nbsp;р./сутки </td><td class="td"></td><td class="td tariff_info-value">6&nbsp;р./сутки </td></tr><tr><td class="td tariff_green ft-XS white">Текущая карточка <br>(6 модулей) </td><td class="td"></td><td class="td tariff_info-value">12&nbsp;р./сутки </td><td class="td"></td><td class="td tariff_info-value">24&nbsp;р./сутки </td><td class="td"></td><td class="td tariff_info-value">36&nbsp;р./сутки </td></tr><tr><td colspan="2"></td><td class="td light-gray ft-XS" colspan="5"> Cоглашением между компанией CBCA и Магазин "cbca" определены тарифные планы на 2017 год. </td></tr></tbody></table></div><div class="ovh"><table class="prop "><tbody><tr><td class="prop_name">Адрес </td><td class="prop_value"> В.О. , Пл. Морской славы д.1 </td></tr></tbody></table><hr class="delimiter __light"></div></div>
                 */

                // Конейнер таблицы тарифов.
                <.div(
                  ^.`class` := Css.Overflow.HIDDEN,
                  <.table(
                    ^.`class` := Css.Lk.Adv.NodeInfo.TARIFF_INFO,
                    <.tbody(

                      // Ряд-заголовок тарифной таблицы.
                      for {
                        tfDailyInfo <- advInfo.tfDaily
                          .orElse { advInfo.tfDaily4Ad.map(_.tfDaily) }
                      } yield {
                        _tableHeaderRow( tfDailyInfo.clauses ): ReactElement
                      },

                      // Ряд с тарифами минимального модуля.
                      for ( tfDailyInfo <- advInfo.tfDaily ) yield {
                        _pricesRow(
                          Messages( MsgCodes.`Minimal.module` ),
                          "(" + Messages( MsgCodes.`scheme.left` ) + ")",
                          tfDailyInfo.clauses
                        ): ReactNode
                      },

                      // Ряд с тарифами в рамках текущей карточки, если есть.
                      for ( adTdDailyInfo <- advInfo.tfDaily4Ad ) yield {
                        _pricesRow(
                          Messages( MsgCodes.`Current.ad` ),
                          Messages( MsgCodes.`N.modules`, adTdDailyInfo.blockModulesCount ),
                          adTdDailyInfo.tfDaily.clauses
                        ): ReactNode
                      },

                      // Строка с малополезным пояснением на тему соглашения между CBCA и теущим узлом:
                      <.tr(
                        <.td(
                          ^.colSpan := 2
                        ),
                        <.td(
                          ^.`class` := Css.flat( Css.Table.Td.TD, Css.Colors.LIGHT_GRAY, Css.Font.Sz.XS ),
                          ^.colSpan := 5,
                          // Рендер предложения "по соглашению с СВСА установлены тарифы ...".
                          Messages(
                            MsgCodes.`Agreement.btw.CBCA.and.node.tariffs.for.year`,
                            "",
                            advInfo.nodeNameBasic,
                            DomQuick.currentYear
                          )
                        )
                      )

                    ) // tbody
                  )   // table
                ),    // div.ovh

                // Список метаданных узла ключ-значение.
                _renderMeta( advInfo.meta )

              )
            )

          )

        }: ReactElement
      }
    }


  }


  val component = ReactComponentB[Props]("NodeAdvInfoPop")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(innerHtmlOptProxy: Props) = component( innerHtmlOptProxy )

}
