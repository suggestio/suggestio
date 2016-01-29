package io.suggest.lk.adv.direct.fsm.states

import io.suggest.lk.adv.direct.fsm.FsmStubT
import io.suggest.lk.adv.direct.m._
import io.suggest.lk.adv.direct.vm.nbar.cities.{CitiesHeads, CurrCityTitle}
import io.suggest.lk.adv.direct.vm.nbar.ngroups.{CityNgs, CityCatNg, CitiesNgs}
import io.suggest.lk.adv.direct.vm.nbar.tabs.{CityTabs, TabCheckBox, CitiesTabs}
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

          // Итератор галочек, которые необходимо выставить вслед за галочкой текущего таба.
          val iter = ngIdOpt.fold [Iterator[CityCatNg]] {
            // Выбраны вообще все места в городе: выставить все галочки во всех заголовках.
            for {
              cityTabs <- CityTabs.find(cityId).iterator
              tabHead  <- cityTabs.tabHeads
              if tabHead.ngId != ngIdOpt
              cb       <- tabHead.checkBox
              if cb.isChecked != isCheckedNow
            } {
              _setCheckedNow(cb)
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
            // Выставить галочки только для группы узлов в рамках текущего города и категории узлов
            val arg = NgBodyId(cityId, ngId = ngId)
            CityCatNg.find(arg)
              .iterator
          }

          for (ng <- iter; cb <- ng.checkBoxes) {
            _setCheckedNow(cb)
          }

          // Запустить пересёт цены.
          _needUpdateData()
        }
      }

    }


    /** Реакция на переключение по конкретных узлов в списке узлов. */
    def _nodeChecked(nc: NodeChecked): Unit = {
      _needUpdateData()
    }

  }

}
