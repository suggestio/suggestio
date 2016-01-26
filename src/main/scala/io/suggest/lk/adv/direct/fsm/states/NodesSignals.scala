package io.suggest.lk.adv.direct.fsm.states

import io.suggest.lk.adv.direct.fsm.FsmStubT
import io.suggest.lk.adv.direct.m.{NgBodyId, CityTabHeadClick, NgClick}
import io.suggest.lk.adv.direct.vm.nbar.cities.{CitiesHeads, CurrCityTitle}
import io.suggest.lk.adv.direct.vm.nbar.ngroups.{CityNgs, CityCatNg, CitiesNgs}
import io.suggest.lk.adv.direct.vm.nbar.nodes.NodeCheckBox
import io.suggest.lk.adv.direct.vm.nbar.tabs.CitiesTabs

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
      // Клик по названию города.
      case cthc: CityTabHeadClick =>
        _cityTabHeadClick(cthc)
      // Клик по галочке возле вкладки категории узлов.
      case ngCl: NgClick =>
        _ngrpClick(ngCl)
    }


    /**
     * Реакция на клик по табу города.
      *
      * @param cthc Сигнал клика по заголовку таба города.
     */
    protected[this] def _cityTabHeadClick(cthc: CityTabHeadClick): Unit = {
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
     * @param ngCl Входящий сигнал о клике.
     */
    protected[this] def _ngrpClick(ngCl: NgClick): Unit = {
      val sd0 = _stateData

      // ngId может быть пустым, значит выбрана вкладка "Все места"
      val ngIdOpt = ngCl.ngHead.ngId

      for {
        cityId <- ngCl.ngHead.cityId
        if sd0.currCityId.contains(cityId)
        ngsCityCont <- CityNgs.find(cityId)
      } {

        if (!sd0.currNgId.contains(ngIdOpt)) {
          // Отобразить контейнер групп узлов текущего города.
          ngsCityCont.show()

          ngIdOpt.fold {
            // Выбрана вкладка "Все места". Нужно все группы в городе найти и отобразить.
            for (ng <- ngsCityCont.ngs) {
              ng.show()
            }
          } { ngId =>
            // Выбрана вкладка конкретной категории узлов. Отобразить её, другие скрыть
            for (ng <- ngsCityCont.ngs) {
              ng.setIsShown(ng.ngId.contains(ngId))
            }
          }

          _stateData = sd0.copy(
            currNgId = Some(ngIdOpt)
          )
        }

        // Если клик был по галочке, то расставить галочки внутри, сменить состояние на запрос цены.
        for (ngCb <- NodeCheckBox.ofEventTarget(ngCl.event.target) ) {
          val isCheckedNow = ngCb.isChecked

          val iter = ngIdOpt.fold [Iterator[CityCatNg]] {
            // TODO Выставить все галочки в NgHeads
            ngsCityCont.ngs
          } { ngId =>
            CityCatNg.find(NgBodyId(cityId, ngId = ngId))
              .iterator
          }

          for (ng <- iter; cb <- ng.checkBoxes) {
            cb.setChecked(isCheckedNow)
          }
        }
      }

    }

  }

}
