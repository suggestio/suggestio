package io.suggest.sc.sjs.c.mapbox

import io.suggest.sc.sjs.m.mmap.EnsureMap
import io.suggest.sc.sjs.vm.search.tabs.geo.SGeoRoot
import io.suggest.sjs.common.msg.WarnMsgs
import io.suggest.sjs.mapbox.gl.map.{GlMap, GlMapOptions}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 22:12
  * Description: FSM-аддон для состояния готовности js к работе.
  */
trait MbgljsReady extends MbFsmStub {

  /** Трейт для сборки состояния готовности mapbox-gl.js к работе на странице. */
  trait MbgljsReadyStateT extends FsmState {

    override def afterBecome(): Unit = {
      super.afterBecome()
      // если в состоянии уже есть ensure, то сделать это
      for (em <- _stateData.ensure) {
        _ensureMap(em)
      }
    }

    override def receiverPart: Receive = {
      // ScFsm намекает о необходимости убедиться, что карта готова к работе.
      case em: EnsureMap =>
        _ensureMap(em)
    }


    /** Инициализация карты в текущей выдаче, если необходимо. */
    def _ensureMap(em: EnsureMap): Unit = {
      val sd0 = _stateData
      if (sd0.glmap.isEmpty) {
        for {
          cont <- SGeoRoot.find()
        } {
          // Пока div контейнера категорий содержит какой-то мусор внутри, надо его очищать перед использованием.
          cont.clear()
          // Собираем опции для работы.
          val opts = GlMapOptions.empty
          opts.container  = cont._underlying
          opts.style      = "mapbox://styles/konstantin2/cimolhvu600f4cunjtnyk1hs6"
          // TODO Воткнуть сюда дефолтовый lat lng и zoom.

          // Инициализировать карту.
          val map0 = new GlMap(opts)

          // Сохранить карту в состояние FSM.
          _stateData = sd0.copy(
            glmap   = Some(map0),
            ensure  = None
          )
        }

      } else {
        warn(WarnMsgs.MAPBOXLG_ALREADY_INIT)
        if (sd0.ensure.nonEmpty) {
          _stateData = sd0.copy(
            ensure = None
          )
        }
      }
    }

  }

}
