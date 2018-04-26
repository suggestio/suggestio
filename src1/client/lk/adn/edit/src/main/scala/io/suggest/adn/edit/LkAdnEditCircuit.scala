package io.suggest.adn.edit

import diode.react.ReactConnector
import io.suggest.adn.edit.api.{ILkAdnEditApi, LKAdnEditApiHttp}
import io.suggest.adn.edit.c.{RootAh, NodeEditAh}
import io.suggest.adn.edit.m._
import io.suggest.lk.c.PictureAh
import io.suggest.lk.m.img.MPictureAh
import io.suggest.msg.ErrorMsgs
import io.suggest.n2.edge.MEdgeDataJs
import io.suggest.primo.id.IId
import io.suggest.routes.routes
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

  import MLkAdnEditRoot.MLkAdnEditRootFastEq
  import MAdnNodeS.MAdnNodeSFastEq
  import MPictureAh.MPictureAhFastEq


  override protected def CIRCUIT_ERROR_CODE = ErrorMsgs.LK_ADN_EDIT_FORM_FAILED

  /** Извлекать начальное состояние формы из html-страницы. */
  override protected def initialModel: MLkAdnEditRoot = {
    val stateInp = StateInp.find().get
    val json = stateInp.value.get
    val minit = Json.parse(json)
      .as[MAdnEditFormInit]

    MLkAdnEditRoot(
      internals = MAdnEditInternals(
        conf = minit.conf
      ),
      node = MAdnNodeS(
        meta          = minit.form.meta,
        colorPresets  = minit.form.meta.colors.allColorsIter.toList,
        edges         = MEdgeDataJs.jdEdges2EdgesDataMap( minit.form.edges ),
        resView       = minit.form.resView
      )
    )
  }


  // Models
  val rootRW = zoomRW(identity(_))((_, root2) => root2)

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

  val internalsRW = zoomRW(_.internals) { _.withInternals(_) }

  val confRO = internalsRW.zoom(_.conf)


  // API
  val uploadApi: IUploadApi = new UploadApiHttp( confRO )

  val lkAdnEditApi: ILkAdnEditApi = new LKAdnEditApiHttp( confRO )


  // Controllers
  val nodeEditAh = new NodeEditAh(
    modelRW = nodeRW
  )

  val pictureAh = {
    import MAdnResViewUtil._
    new PictureAh(
      // Возвращать роуты контроллера LkAdnEdit в зав-ти от предиката и прочих данных из $1?
      prepareUploadRoute = { _ =>
        routes.controllers.LkAdnEdit.uploadImg(
          nodeId = confRO.value.nodeId
        )
      },
      uploadApi = uploadApi,
      modelRW   = mPictureAhRW
    )
  }

  val rootAh = new RootAh(
    api     = lkAdnEditApi,
    modelRW = rootRW
  )


  override protected val actionHandler: HandlerFunction = {
    composeHandlers(
      nodeEditAh,
      pictureAh,
      rootAh
    )
  }

}
