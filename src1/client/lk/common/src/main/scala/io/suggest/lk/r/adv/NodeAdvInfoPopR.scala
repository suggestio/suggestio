package io.suggest.lk.r.adv

import diode.react.ModelProxy
import io.suggest.lk.m.NodeInfoPopupClose
import japgolly.scalajs.react.{BackendScope, Callback, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.adv.info.MNodeAdvInfo
import io.suggest.bill.MPrice
import io.suggest.cal.m.MCalType
import io.suggest.common.html.HtmlConstants
import io.suggest.common.html.HtmlConstants.{`(`, `)`}
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import io.suggest.lk.r.popup.PopupR
import io.suggest.n2.node.meta.MMetaPub
import io.suggest.msg.{JsFormatUtil, Messages}
import io.suggest.react.ReactCommonUtil
import io.suggest.sjs.dom.DomQuick
import react.image.gallery.{IgItem, ImageGalleryPropsR, ImageGalleryR}

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
    private def _tableHeaderRow(clauses: Iterable[(MCalType, MPrice)]): VdomElement = {
      val firstCalTypeOpt = clauses.headOption.map(_._1)

      val td = <.td

      val tail = clauses
        .iterator
        .flatMap { case (mCalType, _) =>
          // Ячейка заголовка с описанием названия (типа) календаря и затрагиваемых дней недели.
          val payloadCell = td(
            ^.key := mCalType.value,
            ^.`class` := Css.flat( Css.Table.Td.TD, Css.Lk.Adv.NodeInfo.TARIFF_INFO_TITLE ),
            Messages( mCalType.i18nCode ),
            HtmlConstants.COLON,
            HtmlConstants.SPACE,
            mCalType.dayStart.whenDefined { dayStart =>
              Messages( MsgCodes.`dayOfW.N.`( dayStart ) )
            },
            HtmlConstants.MINUS.when( mCalType.dayStart.nonEmpty && mCalType.dayEnd.nonEmpty ),
            mCalType.dayEnd.whenDefined { dayEnd =>
              Messages( MsgCodes.`dayOfW.N.`( dayEnd ) )
            }
          )
          val acc0 = payloadCell :: Nil

          // Из-за "особенностей" верстки макса требуется добавлять пустые ячейки между столбцами.
          if ( !firstCalTypeOpt.contains( mCalType ) ) {
            val delimTd = td(
              ^.key := (mCalType.value + HtmlConstants.MINUS)
            )
            delimTd :: acc0
          } else {
            acc0
          }
        }
        .to( LazyList )

      val children =
        td(
          ^.colSpan := 2,
          HtmlConstants.NBSP_STR
        ) #::
        tail

      <.tr(
        children: _*
      )
    }


    /** Ячейки ряда с ценами. */
    private def _pricesRow(rowTitle1: String, rowTitle2: String, clauses: Iterable[(MCalType, MPrice)]): VdomElement = {
      val sutki = Messages( MsgCodes.`day24h` )

      val td = <.td
      val tdCss = Css.Table.Td.TD

      val tail = clauses
        .iterator
        .flatMap { case (mCalType, mPrice) =>
          // td-разделитель, из-за особенностей вёрстки.
          val td1 = td(
            ^.key := (mCalType.value + HtmlConstants.UNDERSCORE),
            ^.`class` := tdCss
          )
          // Непосредственный контент.
          val td2 = td(
            ^.key := mCalType.value,
            ^.`class` := Css.flat( tdCss, Css.Lk.Adv.NodeInfo.TARIFF_INFO_VALUE ),
            JsFormatUtil.formatPrice( mPrice ),
            HtmlConstants.SLASH,
            sutki
          )
          td1 :: td2 :: Nil
        }
        .to( LazyList )

      // Левая колонка: описание принадлежности перечисленных цен.
      val children =
        td(
          ^.`class` := Css.flat( Css.Table.Td.TD, Css.Lk.Adv.NodeInfo.TARIFF_GREEN, Css.Font.Sz.XS, Css.Colors.WHITE ),
          rowTitle1,
          <.br,
          rowTitle2
        ) #::
        tail

      <.tr(
        children: _*
      )
    }


    /** Рендер одного ряда в метаданных. */
    private def _renderMetaRow(msgCode: String, vOpt: Option[String], isUrl: Boolean = false): VdomElement = {
      vOpt.whenDefinedEl { v =>
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
        ): VdomElement
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
        _renderMetaRow( MsgCodes.`Daily.people.traffic`, mmeta.business.humanTraffic.map(_.toString) ),
        _renderMetaRow( MsgCodes.`Audience.descr`, mmeta.business.audienceDescr )
      )
    }


    /** React-only рендер попапа. Исходный рендер работал через innerHtml.
      *
      * @param advInfoOptProxy Прокся props.
      * @return React element.
      */
    def render( advInfoOptProxy: Props ): VdomElement = {
      advInfoOptProxy().whenDefinedEl { advInfo =>
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
                ReactCommonUtil.maybeEl( advInfo.gallery.nonEmpty ) {
                  val hasManyImgs = advInfo.gallery.size >= 2

                  <.div(
                    ^.`class` := Css.Lk.Adv.NodeInfo.TARIFF_PHOTO_LIST,
                    // Юзаем тут react-галеру вместо всяких кривых bxSlider:
                    ImageGalleryR(
                      new ImageGalleryPropsR {
                        override val autoPlay = true
                        override val items: js.Array[IgItem] = {
                          val _imgOriginalClass = Css.Lk.Adv.NodeInfo.TARIFF_PHOTO_IMG
                          val igItems = for {
                            mediaInfo <- advInfo.gallery.iterator
                          } yield {
                            new IgItem {
                              override val original = mediaInfo.url
                              /*override val thumbnail: UndefOr[String] = {
                                mediaInfo.thumb.map(_.url)
                              }*/
                              override val originalClass: UndefOr[String] = {
                                _imgOriginalClass
                              }
                            }: IgItem
                          }
                          igItems.toJSArray
                        }
                        // thumb'ы в дизайне и вёрстке не предусмотрены, с сервера не отсылаются (см. код LkBill2)
                        override val showThumbnails = false
                        // TODO showNav: Надо бы true, но есть какая-то проблема с z-index: кнопки навигации не реагируют на курсор мыши
                        override val showNav = false
                        override val showBullets = hasManyImgs
                        override val slideInterval = 3000
                        override val slideDuration = 450
                        override val showFullscreenButton = true
                      }
                    )
                  )
                },

                // Левая схема-решётка про минимальный модуль карточки.
                <.div(
                  ^.`class` := Css.Lk.Adv.NodeInfo.TARIFF_ILLUSTRATION_W,
                  <.div(
                    ^.`class` := Css.Lk.Adv.NodeInfo.TARIFF_ILLUSTRATION
                  )
                ),

                // Конейнер таблицы тарифов.
                <.div(
                  ^.`class` := Css.Overflow.HIDDEN,
                  <.table(
                    ^.`class` := Css.Lk.Adv.NodeInfo.TARIFF_INFO,
                    <.tbody(

                      // Ряд-заголовок тарифной таблицы.
                      advInfo.tfDaily
                        .orElse { advInfo.tfDaily4Ad.map(_.tfDaily) }
                        .whenDefined { tfDailyInfo =>
                          _tableHeaderRow( tfDailyInfo.clauses ): VdomElement
                        },

                      // Ряд с тарифами минимального модуля.
                      advInfo.tfDaily.whenDefined { tfDailyInfo =>
                        _pricesRow(
                          Messages( MsgCodes.`Minimal.module` ),
                          `(` + Messages( MsgCodes.`scheme.left` ) + `)`,
                          tfDailyInfo.clauses
                        )
                      },

                      // Ряд с тарифами в рамках текущей карточки, если есть.
                      advInfo.tfDaily4Ad.whenDefined { adTdDailyInfo =>
                        _pricesRow(
                          Messages( MsgCodes.`Current.ad` ),
                          Messages( MsgCodes.`N.modules`, adTdDailyInfo.blockModulesCount ),
                          adTdDailyInfo.tfDaily.clauses
                        )
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

        }
      }
    }


  }


  val component = ScalaComponent.builder[Props](getClass.getSimpleName)
    .stateless
    .renderBackend[Backend]
    .build

  def apply(innerHtmlOptProxy: Props) = component( innerHtmlOptProxy )

}
