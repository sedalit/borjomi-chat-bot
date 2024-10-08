theme: /
    
    state: Discounts
        if: getChannelType("chatwidget")
            a: Ознакомиться с акциями вы можете на соответствующей странице {{clickableLink("сайта", "https://mywatershop.ru/action")}}
            go!: /SomethingElse
        else:
            if: !$client.id
                script: $session.cameFrom = "Discounts";
                go!: /Identification
            else:
                script: $temp.discounts = integration.getDicounts()
                if: !$temp.discounts
                    if: $temp.apiStatus === 401
                        script: $session.cameFrom = "Discounts";
                        go!: /Identification
                    else:
                        script: $temp.counter = counter("discounts")
                        if: $temp.counter < 3
                            go!: /Discounts
                        else:
                            go!: /IntegrationFailed
                elseif: $temp.discounts.length === 0
                    a: К сожалению, в данный момент не могу предоставить список акций. Однако ознакомиться с акциями вы можете и на соответствующей странице {{clickableLink("сайта", "https://mywatershop.ru/action")}}
                    go!: /SomethingElse
                else:
                    script: 
                        $session.discountsCatalog = formatDiscounts($temp.discounts);
                        $session.discountsCatalog = catalog.create($session.discountsCatalog);
                    go!: /ShowDiscounts
                    
    state: ShowDiscounts
        q: * (назад|обратно|заново) * || fromState = "/SpecificDiscount"
        script:
            paginateButtons($session.discountsCatalog, {
                "page": pagination.getPage("discounts"),
                "onButtonTransition": "/ShowDiscounts/Fallback",
                "onButtonAction": function(elem) {sendImage(elem.picture)},
                "onNextButtonTransition": "/Pagination/OnNextButton",
                "onPreviousButtonTransition": "/Pagination/OnPreviousButton",
                "onRefreshButtonTransition": "/Pagination/OnRefreshButton"
            });
            
        state: Fallback
            event: noMatch
            if: findItem($session.discountsCatalog, $request.query)
                script: $session.catchedDiscount = findItem($session.discountsCatalog, $request.query);
                go!: /SpecificDiscount
            else:
                script: $temp.counter = counter("showDicsounts");
                if: $temp.counter === 1
                    a: Извините, не совсем понял. Пожалуйста, выберите, какая акция вам интересна?
                    go!: ..
                elseif: $temp.counter === 2
                    a: К сожалению, не смог понять, что вы имеете ввиду. Выберите, пожалуйста, акцию, о которой хотели бы узнать.
                    go!: ..
                else:
                    a: К сожалению, я не могу помочь вам с этим.
                    go!: /TransferToOperator
            
    state: SpecificDiscount
        script: 
            $session.discount = integration.getDiscount($session.catchedDiscount.id);
            discountAnswer($session.discount);
        buttons:
            "Назад" -> /ShowDiscounts
            "Принять участие" -> /ParticipateDiscount
                
        state: Fallback
            event: noMatch
            script: $temp.counter = counter("specificDiscount");
            if: $temp.counter === 1
                script: $temp.fromFallback = true;
                a: Извините, не совсем понял. Пожалуйста, подскажите, выберите, хотели ли вы принять участие в акции?
                go!: ..
            elseif: $temp.counter === 2
                script: $temp.fromFallback = true;
                a: К сожалению, не смог понять, что вы имеете ввиду. Подскажите, хотите принять участие в акции?
                go!: ..
            else:
                a: К сожалению, я не могу помочь вам с этим.
                go!: /TransferToOperator
    
    state: ParticipateDiscount
        q: * (принять участие|хочу участвовать|давай*|(как применить/дай/хочу/давай*) скидк*) * || fromState = "/SpecificDiscount"
        script: $session.transferData = transferData.formatDiscount($session.discount);
        a: Передаю данные об акции оператору. Подождите пару минут, ваш запрос обрабатывается
        go!: /Switch