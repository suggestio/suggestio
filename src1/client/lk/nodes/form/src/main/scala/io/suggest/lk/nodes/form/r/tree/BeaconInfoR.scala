package io.suggest.lk.nodes.form.r.tree

import com.materialui.{Mui, MuiListItem, MuiListItemIcon, MuiListItemProps, MuiListItemText, MuiListItemTextProps}
import diode.react.ModelProxy
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.lk.nodes.MLknNode
import io.suggest.lk.nodes.form.m.{CreateNodeClick, MNodeState, NodesDiConf}
import io.suggest.n2.node.MNodeType
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.ReactDiodeUtil.Implicits._
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sjs.common.empty.JsOptionUtil
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
                     )
  implicit val propsValFeq = FastEqUtil[PropsVal] { (a, b) =>
    (a.nodeState ===* b.nodeState) &&
    (a.isAdvMode ==* b.isAdvMode) &&
    // Инстанс пересобирается постоянно, но внутренние содержимое - статично.
    OptFastEq.Plain.eqv( a.infoOpt, b.infoOpt )
  }

  type Props_t = PropsVal
  type Props = ModelProxy[Props_t]


  class Backend( $: BackendScope[Props, Props_t] ) {

    /** Клик по кнопке добавления маячка в свои узлы. */
    private def _addBtnClick(beaconUid: String, nodeType: MNodeType, nameDflt: Option[String] = None): Callback = {
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, CreateNodeClick(
        id = Some( beaconUid ),
        nameDflt = nameDflt,
        nodeType = Some( nodeType ),
      ))
    }


    def render(propsProxy: Props, s: Props_t): VdomElement = {
      val _isDisabled = s.nodeState.infoPot.isPending
      // Сигнал от маячка: надо отрендерить инфу по выбранному маячку:
      s.nodeState.beacon.whenDefinedEl { bcnState =>
        React.Fragment(

          // Расстояние до маячка:
          distanceCmR.component( propsProxy.resetZoom( bcnState ) ),

          ReactCommonUtil.maybeNode( _isDisabled ) {
            MuiListItem()(
              treeStuffR.LineProgress()
            )
          },

          // Инфа по маячку от сервера.
          (for {
            bcnUid <- bcnState.data.signal.signal.factoryUid
            if s.infoOpt
              // TODO Вернуть exists(), когда будут запросы на сервер за данными по видимым маячкам.
              .fold(true)/*.exists*/( _.ntype.isEmpty )
          } yield {
            // Кнопка "Добавить узел", если infoPot намекает, что данный маячок свободен для привязки.
            val isLoggedIn = nodesDiConf.isUserLoggedIn()
            crCtxP.consume { crCtx =>
              MuiListItem {
                new MuiListItemProps {
                  override val button = true
                  override val onClick = JsOptionUtil.maybeDefined( isLoggedIn ) {
                    ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
                      val signal = bcnState.data.signal.signal
                      val nodeType = signal.typ.nodeType

                      _addBtnClick(
                        beaconUid = bcnUid,
                        nodeType  = nodeType,
                        nameDflt  = signal.customName
                          .orElse {
                            nodeType
                              .creationNameExample
                              .map( crCtx.messages(_) )
                          },
                      )
                    }
                  }
                  override val disabled = _isDisabled
                }
              } (
                MuiListItemIcon()(
                  Mui.SvgIcons.Add()(),
                ),
                {
                  val toRegisterMsg = crCtx.messages( MsgCodes.`_to.Register._thing` )
                  if (isLoggedIn) {
                    // Зареганный юзер - открыть диалог.
                    MuiListItemText(
                      new MuiListItemTextProps {
                        override val primary = toRegisterMsg
                        override val secondary = crCtx.messages( MsgCodes.`Add.beacon.to.account` )
                      }
                    )()
                  } else {
                    // Юзер не залогинен - необходимо отрендерить соответсвующий текст и форму логина при клике:
                    nodesDiConf.needLogInVdom( toRegisterMsg )
                  }
                },
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
