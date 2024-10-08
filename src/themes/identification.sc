theme: /

    state: Identification
        q: * ($no|$disagree) * || fromState = "/GetNumber/Question"
        if: $.session.lastState !== "/GetNumber/Question"
            a: Для дальнейшей консультации требуется авторизация. Пожалуйста, отправьте в чат свой номер телефона для прохождения авторизации
        else:
            a: Пожалуйста, отправьте в чат корректный номер телефона для продолжения авторизации
            
        state: Fallback
            event: noMatch
            script: $temp.counter = counter("identification");
            if: $temp.counter === 1
                a: Извините, не совсем понял. Пожалуйста, отправьте в чат свой номер телефона для прохождения авторизации
            elseif: $temp.counter === 2
                a: К сожалению, не смог понять, что вы имеете в виду. Отправьте, пожалуйста, в чат свой номер телефона для прохождения авторизации
            else:
                a: К сожалению, я не могу помочь вам с этим
                go!: /TransferToOperator
        
    state: GetNumber
        q: * $mobilePhoneNumber * || fromState = "/Identification", onlyThisState = true
        q: * $mobilePhoneNumber * || fromState = "/Identification/Fallback", onlyThisState = true
        if: $.session.lastState !== "/GetCredits/Error"
            script: 
                $client.phoneNumber = validatePhone($request.query);
                $temp.result = integration.auth($client.phoneNumber);
            if: !$temp.result
                if: counter("getNumber") < 3
                    go!: /GetNumber
                else:
                    go!: /GetNumber/Question
            else:
                a: Теперь введите свой логин, который вам отправили
        else:
            a: Теперь введите свой логин, который вам отправили
                    
        state: Question
            if: !$temp.fromFallback
                a: Боюсь, что данный номер не подошёл. Подскажите, вы уверены, что номер {{$client.phoneNumber}} корректный?
            script: $session.lastState = "/GetNumber/Question";
            buttons:
                "Да" -> /GetNumber/Error
                "Нет" -> /Identification
            
            state: Fallback
                event: noMatch
                script: $temp.counter = counter("getNumberQuestion");
                if: $temp.counter === 1
                    script: $temp.fromFallback = true;
                    a: Извините, не совсем понял. Пожалуйста, подскажите, вы уверены, что номер {{$client.phoneNumber}} корректный?
                    go!: ..
                elseif: $temp.counter === 2
                    script: $temp.fromFallback = true;
                    a: К сожалению, не смог понять, что вы имеете в виду. Подскажите, вы уверены, что номер {{$client.phoneNumber}} корректный?
                    go!: ..
                else:
                    a: К сожалению, я не могу помочь вам с этим.
                    go!: /TransferToOperator
                    
        state: Error
            q: * ($yes|$agree) * || fromState = "/GetNumber/Question"
            if: !$temp.fromFallback
                a: К сожалению, не могу найти ваш номер в системе. Подскажите, вы точно регистрировались в MyWaterShop?
            buttons:
                "Да" -> /TransferToOperator
                "Нет" -> /HowToRegistrate
            
            state: Fallback
                event: noMatch
                script: $temp.counter = counter("getNumberError");
                if: $temp.counter === 1
                    script: $temp.fromFallback = true;
                    a: Извините, не совсем понял. Пожалуйста, подскажите, вы регистрировались на сайте MyWaterShop?
                    go!: ..
                elseif: $temp.counter === 1
                    script: $temp.fromFallback = true;
                    a: К сожалению, не смог понять, что вы имеете в виду. Подскажите, вы уже зарегистрированы на сайте MyWaterShop?
                    go!: ..
                else:
                    a: К сожалению, я не могу помочь вам с этим.
                    go!: /TransferToOperator
            
    
    state: GetPassword
        q: * || fromState = "/GetNumber", onlyThisState = true
        script: $session.login = $request.query
        a: Теперь введите пароль, полученный в смс
        
    state: GetCredits
        q: * || fromState = "/GetPassword", onlyThisState = true
        script: 
            integration.hash($session.login, $request.query);
            $temp.client = integration.getClient();
        if: !$temp.client
            if: counter("getCredits") < 3
                go!: /GetCredits
            else:
                go!: /GetCredits/Error
        else:
            script: 
                $client.id = $temp.client.clientid;
                $client.name = $temp.client.name;
                $client.address = $temp.client.shortaddress;
            if: $session.cameFrom == "Discounts"
                go!: /Discounts
            elseif: $session.cameFrom == "Items"
                go!: /Products
            elseif: $session.cameFrom == "OrderTracking"
                go!: /OrderTracking/LookUp
                
        state: Error
            script: 
                $session.lastState = "/GetCredits/Error";
            if: counter("getCreditsError") < 3
                a: К сожалению, не получается получить вас авторизовать. Проверьте, пожалуйста, что вы правильно ввели логин и пароль из смс
                go!: /GetNumber
            else:
                a: Не получается получить информацию о вашей учетной записи
                go!: /TransferToOperator