require: requirements.sc

theme: /

    state: Start
        q!: $regex</start> [$regex<\{.*\}>]
        event: noMatch || fromState = "/Switch/DialogEnd"
        script:
            $jsapi.startSession();
            initVariables();
        random:
            a: Приветствую!
            a: Здравствуйте!
            a: Привет!
        if: $client.wasInteraction
            a: Рад, что вы вернулись! Напомню, я могу ответить на вопросы о сервисе доставке воды MyWatershop и продуктах «Боржоми» и «Святой Источник», которые есть у нас в продаже.
        else:
            a: Меня зовут Иван, я – чатбот сервиса доставки воды «Святой Источник» и «Боржоми» MyWatershop. Я здесь, чтобы ответить на ваши вопросы о сервисе MyWatershop и продуктах «Боржоми» и «Святой Источник», которые есть у нас в продаже
            script: $client.wasInteraction = true
        go!: /HowCanIHelpYou
        
    state: HowCanIHelpYou
        random:
            a: Что бы вы хотели узнать?
            a: Что вас интересует?
            a: Подскажите, какой у вас вопрос?
            a: Могу ли я помочь вам с чем-нибудь?
        go!: /MainButtons
        
        state: Fallback
            event: noMatch
            event: timeLimit
            event: lengthLimit
            script: $temp.counter = counter("howCanIHelpYou");
            if: $temp.counter === 1
                a: Извините, не совсем понял. Пожалуйста, подскажите, чем я могу вам помочь?
                go!: /MainButtons
            elseif: $temp.counter === 2
                a: К сожалению, не смог понять, что вы имеете в виду. Подскажите, что бы вы хотели узнать?
                go!: /MainButtons
            else:
                a: К сожалению, я не могу помочь вам с этим
                go!: /TransferToOperator
                
    state: MainButtons || noContext = true
        if: getChannelType("chatwidget")
            buttons:
                "Где мой заказ?" -> /OrderTracking/Number
                "О MyWaterShop" -> /AboutMyWaterShop
                "Онлайн-консультация" -> /TransferToOperator
        elseif: getChannelType("whatsapp")
            buttons:
                "Акции" -> /Discounts
                "Товары" -> /Products
                "Где мой заказ?" -> /OrderTracking/Number
        else:
            buttons:
                "Акции" -> /Discounts
                "Товары" -> /Products
                "Где мой заказ?" -> /OrderTracking/Number
                "О MyWaterShop" -> /AboutMyWaterShop
                "Онлайн-консультация" -> /TransferToOperator            
                
    state: SomethingElse
        q: * ($no|$disagree) * || fromState = "/OrderTracking/Error/No"
        random:
            a: Что бы вы хотели узнать ещё?
            a: Что вас ещё интересует?
            a: Могу ли я помочь вам с чем-нибудь ещё?
        go!: /MainButtons
        
        state: Fallback
            event: noMatch
            event: timeLimit
            event: lengthLimit
            script: $temp.counter = counter("somethingElse");
            if: $temp.counter === 1
                a: Извините, не совсем понял. Пожалуйста, подскажите, чем я могу вам помочь?
                go!: /MainButtons
            elseif: $temp.counter === 2
                a: К сожалению, не смог понять, что вы имеете в виду. Подскажите, что бы вы хотели узнать?
                go!: /MainButtons
            else:
                a: К сожалению, я не могу помочь вам с этим
                go!: /TransferToOperator
                
    state: HowToRegistrate
        intent!: /Как зарегистрироваться
        q: * ($no|$disagree) * || fromState = "/GetNumber/Error"
        a: Пройти регистрацию для получения логина и пароля или восстановить пароль вы можете на сайте MyWaterShop или через оператора. Подскажите, как вариант был бы вам более удобным?
        buttons:
            "Через сайт" -> /HowToRegistrate/Online
            "Через оператора" -> /TransferToOperator
        
        state: Online
            q: * ([через/на] сайт*/[в] (онлайн*/он лайн*)) *
            a: Для регистрации вы можете перейти на страницу {{clickableLink("регистрации", "https://mywatershop.ru/profile/register")}}. Для восстановления пароля перейдите на вкладку "Войти" и выберите пункт "Я забыл пароль"
            go!: /SomethingElse
            
    state: IntegrationFailed
        a: К сожалению в данный момент не удалось получить запрашиваемую информацию. Вы можете попробовать повторить ваш запрос через 5-10 минут, а также получить необходимую информацию через оператора или {{clickableLink("сайт", "https://mywatershop.ru/")}}
        go!: /SomethingElse