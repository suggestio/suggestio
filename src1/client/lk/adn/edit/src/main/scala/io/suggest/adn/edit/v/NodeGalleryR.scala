package io.suggest.adn.edit.v

import diode.FastEq
import diode.react.ModelProxy
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import io.suggest.lk.m.frk.{MFormResourceKey, MFrkTypes}
import io.suggest.lk.r.UploadStatusR
import io.suggest.lk.r.img.{ImgEditBtnPropsVal, ImgEditBtnR}
import io.suggest.model.n2.edge.MPredicates
import io.suggest.msg.Messages
import io.suggest.react.ReactDiodeUtil
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.spa.FastEqUtil.DeepCollFastEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.04.18 15:44
  * Description:
  */
class NodeGalleryR(
                    val imgEditBtnR     : ImgEditBtnR,
                    val uploadStatusR   : UploadStatusR
                  ) {

  import io.suggest.spa.OptFastEq.Wrapped
  import io.suggest.file.up.MFileUploadS.MFileUploadSFastEq
  import io.suggest.lk.r.img.ImgEditBtnPropsVal.ImgEditBtnRPropsValFastEq

  lazy val resKeyTypeSome = Some( MFrkTypes.GalImg )

  type Props_t = Seq[PropsValEl]
  type Props = ModelProxy[Props_t]

  case class PropsValEl(
                         editBtn        : imgEditBtnR.Props_t,
                         uploadStatus   : uploadStatusR.Props_t
                       )
  /** Условно-быстрое сравнивание списка галерных картинок узла. */
  implicit object NodeGalleryRPropsValElFastEq extends FastEq[PropsValEl] {
    override def eqv(a: PropsValEl, b: PropsValEl): Boolean = {
      implicitly[FastEq[imgEditBtnR.Props_t]].eqv( a.editBtn, b.editBtn ) &&
      implicitly[FastEq[uploadStatusR.Props_t]].eqv( a.uploadStatus, b.uploadStatus )
    }
  }


  val galPredSome = Some( MPredicates.GalleryItem )
  val imgsRowContCss = List( Css.Floatt.LEFT )

  /** Пропертисы кнопки добавления элемента сейчас являются константой. */
  private val _addBtnProps = ImgEditBtnPropsVal(
    src = None,
    resKey = MFormResourceKey(
      frkType = resKeyTypeSome
    ),
    css = imgsRowContCss
  )

  class Backend($: BackendScope[Props, Props_t]) {

    def render(propsProxy: Props): VdomElement = {
      val galImgs = propsProxy.value

      <.div(
        ^.`class` := Css.PropTable.TABLE,

        // Заголовок строки (слева).
        <.p(
          ^.`class` := Css.flat( Css.PropTable.TD_NAME, Css.PropTable.BLOCK ),
          Messages( MsgCodes.`Node.photos` ),
          HtmlConstants.COLON
        ),

        // Кнопка добавления новой фотки
        propsProxy.wrap(_ => _addBtnProps)( imgEditBtnR.apply ),

        // Уже добавленный фотки галереи:
        <.div(
          (for {
            (galImgS, i) <- galImgs
              .iterator
              .zipWithIndex
          } yield {
            <.div(
              ^.key := i.toString,
              propsProxy.wrap(_ => galImgS.editBtn)( imgEditBtnR.apply ),
              propsProxy.wrap(_ => galImgS.uploadStatus)( uploadStatusR.apply )
            )
          }).toVdomArray
        )

      )

    }
  }


  val component = ScalaComponent.builder[Props](getClass.getSimpleName)
    .initialStateFromProps( ReactDiodeUtil.modelProxyValueF )
    .renderBackend[Backend]
    .configure( ReactDiodeUtil.statePropsValShouldComponentUpdate( DeepCollFastEq[PropsValEl, Seq] ) )
    .build

  def apply(propsValSeqProxy: Props) = component( propsValSeqProxy )

}
