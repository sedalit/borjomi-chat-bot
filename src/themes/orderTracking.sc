theme: /OrderTracking

    state: Number
        q: * ($no|$disagree) * || fromState = "/OrderTracking/Question"
        if: $session.lastState !== "/OrderTracking/Question"
            a: Могу помочь вам отследить заказ. Пожалуйста, укажите номер заказа, и я посмотрю. Напомню, что в нем должно быть 8 цифр.
        else:
            a: Пожалуйста, отправьте в чат корректный номер заказа.
        script: $session.orderNumber = null;

    state: LookUp
        q: @orderNumber || fromState = "/OrderTracking/Number"
        if: $session.lastState !== "/GetCredits"
            script: $session.orderNumber = $session.orderNumber || $parseTree.orderNumber[0].value;
        if: !$client.id
            script: $session.cameFrom = "OrderTracking"
            go!: /Identification
        else:
            script: $temp.lastOrder = integration.getLastOrder()
            if: !$temp.lastOrder
                if: $temp.apiStatus === 401
                    script: $session.cameFrom = "OrderTracking"
                    go!: /Identification
                else:
                    if: counter("lookUp") < 3
                        go!: /OrderTracking/LookUp
                    else:
                        go!: /OrderTracking/Error
            elseif: $temp.lastOrder.id !== $session.orderNumber
                go!: /OrderTracking/Question
            else:
                a: Нашёл статус вашего последнего заказа!
                a: {{lastOrderAnswer($temp.lastOrder)}}
                go!: /SomethingElse

    state: Fallback
        event: noMatch
        script: $temp.counter = counter("number");
        if: $temp.counter === 1
            a: Извините, не совсем понял. Пожалуйста, сообщите номер заказа из 8 цифр в формате ХХХХХХХХ
        elseif: $temp.counter === 2
            a: К сожалению, не смог понять, что вы имеете в виду. Подскажите, номер заказа из 8 цифр в формате ХХХХХХХХ
        else:
            a: К сожалению, я не могу помочь вам с этим
            go!: /TransferToOperator

    state: Question
        a: Боюсь, что не получается найти заказ с таким номером. Подскажите, вы уверены, что номер заказа {{$session.orderNumber}} корректный?
        buttons:
            "Да" -> /OrderTracking/Error
            "Нет" -> /OrderTracking/Number

    state: Error
        q: * ($yes|$agree) * || fromState = "/OrderTracking/Question"
        if: !$temp.fromFallback
            a: К сожалению, не могу найти у вас последний заказ. Подскажите, вы точно делали заказ в MyWaterShop?
        buttons:
            "Да" -> /TransferToOperator
            "Нет" -> /OrderTracking/Error/No

        state: No
            q: * ($no|$disagree) *
            if: !$temp.fromFallback
                a: Тогда я не смогу вам помочь с этим. Хотите ознакомиться с ассортиментом нашей продукции?
            buttons:
                "Да" -> /Products
                "Нет" -> /SomethingElse

            state: Fallback
                event: noMatch || fromState = "/OrderTracking/Error/No", onlyThisState = true
                script: $temp.counter = counter("errorNo")
                if: $temp.counter === 1
                    script: $temp.fromFallback = true
                    a: Извините, не совсем понял. Пожалуйста, подскажите, хотите посмотреть, какие товары мы можем предложить?
                    go!: ..
                elseif: $temp.counter === 2
                    script: $temp.fromFallback = true
                    a: К сожалению, не смог понять, что вы имеете в виду. Подскажите, что бы вы хотели узнать, что можно приобрести в MyWaterShop?
                    go!: ..
                else:
                    a: К сожалению, я не могу помочь вам с этим.
                    go!: /TransferToOperator
        
        state: Fallback
            event: noMatch || fromState = "/OrderTracking/Error", onlyThisState = true
            script: $temp.counter = counter("error")
            if: $temp.counter === 1
                script: $temp.fromFallback = true
                a: Извините, не совсем понял. Пожалуйста, подскажите, вы делали заказ в MyWaterShop?
                go!: ..
            elseif: $temp.counter === 2
                script: $temp.fromFallback = true
                a: К сожалению, не смог понять, что вы имеете в виду. Подскажите, вы уже что-то заказывали в MyWaterShop?
                go!: ..
            else:
                a: К сожалению, я не могу помочь вам с этим.
                go!: /TransferToOperator