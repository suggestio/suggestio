package io.suggest.lk.nodes.form.r.tree

import com.materialui.{Mui, MuiListItem, MuiListItemIcon, MuiListItemProps, MuiListItemText, MuiListItemTextProps}
import diode.react.ModelProxy
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.lk.nodes.MLknNode
import io.suggest.lk.nodes.form.m.{CreateNodeClick, MBeaconCachedEntry, MNodeState, NodesDiConf}
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.ReactDiodeUtil.Implicits._
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.spa.{FastEqUtil, OptFastEq}
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.09.2020 12:54
  * Description: Рендер инфы по bluetooth-маячку. На выходе - List items.
  * Если это не маячок - ничего рендерить не требуется.
  */
class BeaconInfoR(
                   distanceCmR          : DistanceRowR,
                   nodesDiConf          : NodesDiConf,
                   treeStuffR           : TreeStuffR,
                   crCtxP               : React.Context[MCommonReactCtx],
                 ) {

  case class PropsVal(
                       nodeState  : MNodeState,
                       isAdvMode  : Boolean,
                       infoOpt    : Option[MLknNode],
                       bcnCache   : Map[String, MBeaconCachedEntry],
                     )
  implicit val propsValFeq = FastEqUtil[PropsVal] { (a, b) =>
    (a.nodeState ===* b.nodeState) &&
    (a.isAdvMode ==* b.isAdvMode) &&
    // Инстанс пересобирается постоянно, но внутренние содержимое - статично.
    OptFastEq.Plain.eqv( a.infoOpt, b.infoOpt ) &&
    // TODO Opt Карта кэша меняется чаще, чем стоило бы пере-рендеривать текущий элемент.
    (a.bcnCache  ===* b.bcnCache)
  }

  type Props_t = PropsVal
  type Props = ModelProxy[Props_t]


  class Backend( $: BackendScope[Props, Props_t] ) {

    /** Клик по кнопке добавления маячка в свои узлы. */
    private def _addBtnClick(beaconUid: String, nameDflt: Option[String] = None): Callback = {
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, CreateNodeClick(
        id = Some( beaconUid ),
        nameDflt = nameDflt,
      ))
    }


    def render(propsProxy: Props, s: Props_t): VdomElement = {
      // Сигнал от маячка: надо отрендерить инфу по выбранному маячку:
      s.nodeState.beacon.whenDefinedEl { bcnState =>
        React.Fragment(

          // Расстояние до маячка:
          distanceCmR.component( propsProxy.resetZoom( bcnState ) ),

          // Инфа по маячку от сервера.
          if (s.nodeState.infoPot.isPending) {
            MuiListItem()(
              treeStuffR.LineProgress()
            )

          } else if (s.nodeState.infoPot.isUnavailable) {
            // Отмеченный недоступностью узел намекает, что есть какое-то препятствие для получения информации с сервера.
            // Например, незалогиенность юзера в случае, когда форма отрендерена внутри выдачи.
            MuiListItem()(
              MuiListItemIcon()(
                Mui.SvgIcons.Warning()(),
              ),
              if (nodesDiConf.isUserLoggedIn()) {
                MuiListItemText()(
                  crCtxP.message( MsgCodes.`Something.gone.wrong` )
                )
              } else {
                nodesDiConf.needLogInVdom()
              },
            )

          } else (for {
            bcnUid <- bcnState.data.detect.signal.beaconUid
            if s.infoOpt
              // TODO Вернуть exists(), когда будут запросы на сервер за данными по видимым маячкам.
              .fold(true)/*.exists*/( _.ntype.isEmpty )
          } yield {
            // Кнопка "Добавить узел", если infoPot намекает, что данный маячок свободен для привязки.
            crCtxP.consume { crCtx =>
              MuiListItem {
                val _onClickF = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
                  val nameDflt = crCtx.messages( MsgCodes.`Beacon.name.example` )
                  _addBtnClick( bcnUid, Some(nameDflt) )
                }
                new MuiListItemProps {
                  override val button = true
                  override val onClick = _onClickF
                }
              } (
                MuiListItemIcon()(
                  Mui.SvgIcons.Add()(),
                ),
                MuiListItemText(
                  new MuiListItemTextProps {
                    override val primary = crCtx.messages( MsgCodes.`_to.Register._thing` )
                    override val secondary = crCtx.messages( MsgCodes.`Add.beacon.to.account` )
                  }
                )(),
              )
            }
          })
            .whenDefinedEl,

          // Вывод остальной инфы по зареганному узлу - это в NodeR-компонентах.
        )
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps( ReactDiodeUtil.modelProxyValueF )
    .renderBackend[Backend]
    .configure( ReactDiodeUtil.statePropsValShouldComponentUpdate( propsValFeq ) )
    .build

}
