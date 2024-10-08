theme: /

    state: FAQ.Common
        intentGroup!: /KnowledgeBase/FAQ.Common
        script: $faq.pushReplies();
        go!: /SomethingElse
        
    state: FAQ.Web
        intentGroup!: /KnowledgeBase/FAQ.Web
        script: $faq.pushReplies();
        go!: /SomethingElse
        
    state: FAQ.Transfer
        intentGroup!: /KnowledgeBase/FAQ.Transfer
        go!: /TransferToOperator
        
    state: AboutMyWaterShop
        script: $faq.pushReplies("/KnowledgeBase/FAQ.Common/Root/AboutMyWaterShop");
        go!: /SomethingElse