package io.suggest.adn.edit

import diode.react.ReactConnector
import io.suggest.adn.edit.c.NodeEditAh
import io.suggest.adn.edit.m._
import io.suggest.lk.c.PictureAh
import io.suggest.lk.m.img.MPictureAh
import io.suggest.msg.ErrorMsgs
import io.suggest.n2.edge.MEdgeDataJs
import io.suggest.primo.id.IId
import io.suggest.sjs.common.log.CircuitLog
import io.suggest.spa.StateInp
import play.api.libs.json.Json
import io.suggest.ueq.UnivEqUtil._
import io.suggest.up.{IUploadApi, UploadApiHttp}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.04.18 17:35
  * Description: Circuit для формы редактирования ADN-узла.
  */
class LkAdnEditCircuit
  extends CircuitLog[MLkAdnEditRoot]
  with ReactConnector[MLkAdnEditRoot]
{

  override protected def CIRCUIT_ERROR_CODE = ErrorMsgs.LK_ADN_EDIT_FORM_FAILED

  /** Извлекать начальное состояние формы из html-страницы. */
  override protected def initialModel: MLkAdnEditRoot = {
    val stateInp = StateInp.find().get
    val json = stateInp.value.get
    val minit = Json.parse(json)
      .as[MAdnEditFormInit]

    MLkAdnEditRoot(
      conf = minit.conf,
      node = MAdnNodeS(
        meta          = minit.form.meta,
        colorPresets  = minit.form.meta.colors.allColorsIter.toList,
        edges         = IId.els2idMap( minit.form.edges.iterator.map(MEdgeDataJs(_)) ),
        resView       = minit.form.resView
      )
    )
  }


  // Models
  val nodeRW = zoomRW(_.node)(_.withNode(_))

  val mPictureAhRW = zoomRW [MPictureAh[MAdnResView]] { mroot =>
    MPictureAh(
      edges       = mroot.node.edges,
      view        = mroot.node.resView,
      errorPopup  = mroot.popups.errorPopup,
      cropPopup   = mroot.popups.cropPopup,
      histograms  = Map.empty,
      cropContSz  = None
    )
  } { (mroot0, mPictureAh2) =>
    var mroot2 = mroot0

    // Импортировать изменившиеся node-поля:
    if ((mroot0.node.edges !===* mPictureAh2.edges) ||
        (mroot0.node.resView !===* mPictureAh2.view)) {
      val node2 = mroot0.node.copy(
        edges   = mPictureAh2.edges,
        resView = mPictureAh2.view
      )
      mroot2 = mroot2.withNode( node2 )
    }

    // Импортировать изменившиеся попапы:
    if ((mroot2.popups.cropPopup !===* mPictureAh2.cropPopup) ||
        (mroot2.popups.errorPopup !===* mPictureAh2.errorPopup)) {
      val popups2 = mroot2.popups.copy(
        cropPopup = mPictureAh2.cropPopup,
        errorPopup = mPictureAh2.errorPopup
      )
      mroot2 = mroot2.withPopups( popups2 )
    }

    mroot2
  }

  val confRO = zoom(_.conf)


  // Controllers
  val nodeEditAh = new NodeEditAh(
    modelRW = nodeRW
  )

  // TODO Нужен ctxId, который можно через conf передать.
  val uploadApi: IUploadApi = new UploadApiHttp(confRO)

  val pictureAh = {
    import MAdnResViewUtil._
    new PictureAh(
      prepareUploadRoute = { mrk =>
        // TODO Возвращать роуты контроллера LkAdnEdit в зав-ти от предиката и прочих данных из mrk
        ???
      },
      uploadApi = uploadApi,
      modelRW = mPictureAhRW
    )
  }

  override protected val actionHandler: HandlerFunction = {
    composeHandlers(
      nodeEditAh,
      pictureAh
    )
  }

}
