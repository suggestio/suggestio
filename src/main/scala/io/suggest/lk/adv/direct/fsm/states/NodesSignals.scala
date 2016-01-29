package io.suggest.lk.adv.direct.fsm.states

import io.suggest.lk.adv.direct.fsm.FsmStubT
import io.suggest.lk.adv.direct.m._
import io.suggest.lk.adv.direct.vm.nbar.cities.{CitiesHeads, CurrCityTitle}
import io.suggest.lk.adv.direct.vm.nbar.ngroups.{CityNgs, CityCatNg, CitiesNgs}
import io.suggest.lk.adv.direct.vm.nbar.tabs._
import io.suggest.sjs.common.vm.input.Checked

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.12.15 14:37
 * Description: Восприятие сигналов формы от колонки нод (узлов).
 */
trait NodesSignals extends FsmStubT {

  protected[this] trait NodesSignalsStateT extends FsmEmptyReceiverState {

    /** Ресивер сигналов от формы нод. */
    override def receiverPart: Receive = super.receiverPart orElse {
      // Клик по конкретному узлу в списке узлов.
      case nc: NodeChecked =>
        _nodeChecked(nc)

      // Клик по галочке возле вкладки категории узлов.
      case ngCl: NgClick =>
        _ngrpClick(ngCl)

      // Клик по названию города.
      case cthc: CityTabHeadClick =>
        _cityTabHeadClick(cthc)

      // Изменение галочки привелегированного бесплатного безлимитного размещения.
      case _: Adv4FreeChanged =>
        _needUpdateData()
    }


    /**
      * Реакция на клик по табу города.
      *
      * @param cthc Сигнал клика по заголовку таба города.
      */
    def _cityTabHeadClick(cthc: CityTabHeadClick): Unit = {
      val sd0 = _stateData

      // Скрыть список городов
      for (cHeads <- CitiesHeads.find()) {
        cHeads.hide()
      }

      // Проверить, изменился ли город?
      for (cityId <- cthc.cityTabHead.cityId if !sd0.currCityId.contains(cityId)) {

        // Выставить выбранный город в заголовок списка городов.
        for (currCityTitle <- CurrCityTitle.find()) {
          val html = cthc.cityTabHead.innerHtml
          currCityTitle.setContent(html)
        }

        // TODO Скрыть вообще все группы узлов, т.к. они остались в предыдущем городе.
        for {
          ngCities <- CitiesNgs.find()
          ngCity   <- ngCities.cities
        } {
          ngCity.hide()
        }

        // Переключиться на отображение узлов интересующего города, скрыв остальные группы узлов с экрана.
        for (cBodies <- CitiesTabs.find()) {
          cBodies.hide()
          for (cityBody <- cBodies.bodies) {
            val isShown = cityBody.cityId.contains(cityId)
            cityBody.setIsShown(isShown)
          }
          cBodies.show()
        }

        // Обновить состояние
        _stateData = sd0.copy(
          currCityId = Some(cityId)
        )
      }
    }


    /**
      * Реакция на клик по заголовку вкладки группы узлов.
      *
      * @param ngCl Входящий сигнал о клике.
      */
    def _ngrpClick(ngCl: NgClick): Unit = {
      val sd0 = _stateData

      // ngId может быть пустым, значит выбрана вкладка "Все места"
      val ngIdOpt = ngCl.ngHead.ngId

      for {
        cityId <- ngCl.ngHead.cityId
        if sd0.currCityId.contains(cityId)
        ngsCityCont <- CityNgs.find(cityId)
      } {

        // Если сменился текущий таб...
        if (!sd0.currNgId.contains(ngIdOpt)) {
          // Отобразить контейнер групп узлов текущего города.
          ngsCityCont.show()

          ngIdOpt.fold {
            // Выбрана вкладка "Все места". Нужно все группы в городе найти и отобразить.
            for (ng <- ngsCityCont.nodeGroups) {
              ng.show()
            }
          } { ngId =>
            // Выбрана вкладка конкретной категории узлов. Отобразить её, другие скрыть
            for (ng <- ngsCityCont.nodeGroups) {
              ng.setIsShown(ng.ngId.contains(ngId))
            }
          }

          _stateData = sd0.copy(
            currNgId = Some(ngIdOpt)
          )
        }

        // Если клик был по галочке таба, то найти обновить зависимые галочки.
        for (ngCb <- TabCheckBox.ofEventTarget(ngCl.event.target) ) {
          val isCheckedNow = ngCb.isChecked

          // Дедубликация кода выставления галочки в Checked VM.
          def _setCheckedNow(cb: Checked) = cb.setChecked(isCheckedNow)

          // Дедубликация кода обновления счетчика таба.
          def _updateCounter(ctrOpt: Option[TabCounter]): Unit = {
            for (ctr <- ctrOpt) {
              if (isCheckedNow) {
                for (ta <- ctr.totalAvail)
                  ctr.setCounter(ta)
              } else {
                ctr.unsetCounter()
              }
            }
          }

          // Узнать итератор галочек нод, которые необходимо выставить вслед за галочкой текущего таба.
          val iter = ngIdOpt.fold [Iterator[CityCatNg]] {
            // Выбраны вообще все места в городе: выставить все галочки во всех заголовках.
            for {
              cityTabs <- CityTabs.find(cityId).iterator
              tabHead  <- cityTabs.tabHeads
              if tabHead.ngId != ngIdOpt
              cb       <- tabHead.checkBox
            } {
              // Выставить галочку на табе, если ещё не выставлена.
              if (cb.isChecked != isCheckedNow)
                _setCheckedNow(cb)

              // Обновить счетчик узлов на табе
              _updateCounter(tabHead.counter)
            }
            // Вернуть все группы узлов
            ngsCityCont.nodeGroups

          } { ngId =>
            // Если снята галочка на группе, то убрать галочку на "все узлы"
            if (!isCheckedNow) {
              for {
                allCb <- TabCheckBox.find( CityNgIdOpt(cityId, None) )
                if allCb.isChecked
              } {
                _setCheckedNow(allCb)
              }
            }

            // Обновить счетчик на текущем табе
            val ctrOpt = TabCounter.find( CityNgIdOpt(cityId, Some(ngId)) )
            _updateCounter(ctrOpt)

            // Выставить галочки только для группы узлов в рамках текущего города и категории узлов
            val arg = NgBodyId(cityId, ngId = ngId)
            CityCatNg.find(arg)
              .iterator
          }

          // Обновить галочки напротив всех узлов, группы которых изменили свой статус
          for {
            ng <- iter
            nr <- ng.nodeRows
            cb <- nr.checkBox
          } {
            _setCheckedNow(cb)
          }

          // Запустить пересёт цены.
          _needUpdateData()
        }
      }

    }


    /** Реакция на переключение по конкретных узлов в списке узлов. */
    def _nodeChecked(nc: NodeChecked): Unit = {
      // Изменился состав узлов: пора пересчитать ценник.
      _needUpdateData()

      // TODO Надо обновить счетчик на вкладке узла и, возможно, галочку на вкладке/вкладках
      for {
        nr      <- nc.ncb.nodeRow
        nodeId  <- nr.nodeId
        ng      <- nr.nodeGroup
        tabHead <- ng.tabHead
        ctr     <- tabHead.counter
      } {
        // Счетчик каждый раз вычисляется заново. Для надежности.
        // Посчитать текущие галочки на текущей странице-вкладке:
        val checkedTotal = {
          ng.nodeRows
            .flatMap(_.checkBox)
            .count(_.isChecked)
        }

        // Выставить новое значение во вкладку
        ctr.maybeSetCounter(checkedTotal)

        // Выставить галочку во вкладку
        for {
          ta  <- ctr.totalAvail
          tcb <- tabHead.checkBox
        } {
          tcb.setChecked( checkedTotal >= ta )
        }
      }
    }

  }

}
