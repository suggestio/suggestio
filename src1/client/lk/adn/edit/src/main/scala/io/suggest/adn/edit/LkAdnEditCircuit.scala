package io.suggest.adn.edit

import diode.react.ReactConnector
import io.suggest.spa.CircuitUtil
import io.suggest.adn.edit.api.{ILkAdnEditApi, LKAdnEditApiHttp}
import io.suggest.adn.edit.c.{NodeEditAh, RootAh}
import io.suggest.adn.edit.m._
import io.suggest.color.{IColorPickerMarker, MColorType, MColorTypes}
import io.suggest.lk.c.{ColorPickAh, PictureAh}
import io.suggest.lk.m.color.{MColorPick, MColorsState}
import io.suggest.lk.m.img.MPictureAh
import io.suggest.n2.node.meta.MMetaPub
import io.suggest.msg.ErrorMsgs
import io.suggest.n2.edge.MEdgeDataJs
import io.suggest.routes.routes
import io.suggest.sjs.common.log.CircuitLog
import io.suggest.spa.{OptFastEq, StateInp}
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

  import MPictureAh.MPictureAhFastEq
  import MColorPick.MColorPickFastEq


  override protected def CIRCUIT_ERROR_CODE = ErrorMsgs.LK_ADN_EDIT_FORM_FAILED

  /** Извлекать начальное состояние формы из html-страницы. */
  override protected def initialModel: MLkAdnEditRoot = {
    val stateInp = StateInp.find().get
    val json = stateInp.value.get
    val minit = Json.parse(json)
      .as[MAdnEditFormInit]

    MLkAdnEditRoot(
      internals = MAdnEditInternals(
        conf = minit.conf,
        colorState = MColorsState(
          colorPresets  = minit.form.meta.colors.allColorsIter.toList,
        ),
      ),
      node = MAdnNodeS(
        meta          = minit.form.meta,
        edges         = MEdgeDataJs.jdEdges2EdgesDataMap( minit.form.edges ),
        resView       = minit.form.resView
      )
    )
  }


  // Models
  private[edit] val rootRW = zoomRW(identity(_))((_, root2) => root2)

  private val nodeRW = CircuitUtil.mkLensRootZoomRW(this, MLkAdnEditRoot.node)(MAdnNodeS.MAdnNodeSFastEq)

  private val mPictureAhRW = zoomRW [MPictureAh[MAdnResView]] { mroot =>
    MPictureAh(
      edges       = mroot.node.edges,
      view        = mroot.node.resView,
      errorPopup  = mroot.popups.errorPopup,
      cropPopup   = mroot.popups.cropPopup,
      histograms  = Map.empty
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
      mroot2 = (MLkAdnEditRoot.node set node2)(mroot2)
    }

    // Импортировать изменившиеся попапы:
    if ((mroot2.popups.cropPopup !===* mPictureAh2.cropPopup) ||
        (mroot2.popups.errorPopup !===* mPictureAh2.errorPopup)) {
      val popups2 = mroot2.popups.copy(
        cropPopup = mPictureAh2.cropPopup,
        errorPopup = mPictureAh2.errorPopup
      )
      mroot2 = (MLkAdnEditRoot.popups set popups2)(mroot2)
    }

    mroot2
  }

  private val internalsRW = CircuitUtil.mkLensRootZoomRW(this, MLkAdnEditRoot.internals)( MAdnEditInternals.MAdnEditInternalsFastEq )

  private val confRO = CircuitUtil.mkLensZoomRO(internalsRW, MAdnEditInternals.conf)


  // API
  private val uploadApi: IUploadApi = new UploadApiHttp( confRO )

  private val lkAdnEditApi: ILkAdnEditApi = new LKAdnEditApiHttp( confRO )


  // Controllers
  private val nodeEditAh = new NodeEditAh(
    modelRW = nodeRW
  )

  /** Сборка инстансов контроллера ColorPickAh для разных picker'ов цвета фона и переднего плана. */
  private def _mkColorPickerAh(colorOfType: MColorType with IColorPickerMarker): ColorPickAh[MLkAdnEditRoot] = {
    new ColorPickAh[MLkAdnEditRoot](
      myMarker = Some(colorOfType),
      modelRW  = zoomRW [Option[MColorPick]] { mroot =>
        // TODO Выкинуть этот Option, он не нужен.
        val mcp = MColorPick(
          colorOpt    = mroot.node.meta.colors.ofType(colorOfType),
          colorsState = mroot.internals.colorState
        )
        Some(mcp)
      } { (mroot0, mcpOpt2) =>
        mcpOpt2.fold(mroot0) { mcp2 =>
          (
            MLkAdnEditRoot.node
              .composeLens( MAdnNodeS.meta )
              .composeLens( MMetaPub.colors )
              .modify( _.withColorOfType(colorOfType, mcp2.colorOpt) ) andThen
            MLkAdnEditRoot.internals
              .composeLens( MAdnEditInternals.colorState )
              .set( mcp2.colorsState )
          )(mroot0)
        }
      }( OptFastEq.Wrapped(MColorPickFastEq) )
    )
  }

  private val bgColorPickAh = _mkColorPickerAh( MColorTypes.Bg )
  private val fgColorPickAh = _mkColorPickerAh( MColorTypes.Fg )

  private val pictureAh = {
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

  private val rootAh = new RootAh(
    api     = lkAdnEditApi,
    modelRW = rootRW
  )


  override protected val actionHandler: HandlerFunction = {
    composeHandlers(
      bgColorPickAh,
      fgColorPickAh,
      nodeEditAh,
      pictureAh,
      rootAh
    )
  }

}
