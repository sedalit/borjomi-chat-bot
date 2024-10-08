var integration = new function() {
    this.init = function() {
        var injector = $.injector;
        var mode = injector.api.mode;
        $http.config({
            url: {
                protocol: injector.api[mode].protocol || "https",
                host: injector.api[mode].host,
                port: injector.api[mode].port || 443
            },
            cacheTimeToLiveInSeconds: injector.api[mode].cacheTimeInSeconds || 0
        });
    }
    
    this.request = function(httpMethod, apiMethod, paramsObj) {
        var url = "/" + $.injector.api.version + apiMethod;
        paramsObj.headers = paramsObj.headers || {};
        if ($.client.hash) paramsObj.headers['Authorization'] = "Basic " + $.client.hash;
        var response = $http.query(url, {
            method: httpMethod.toUpperCase(),
            body: paramsObj.body || {},
            headers: paramsObj.headers || {},
        });
        $.temp.apiStatus = response.status;
        this.log(response, httpMethod, apiMethod, paramsObj);
        return response.isOk ? response : false;
    }
    
    this.auth = function(phone) {
        return this.request('post', '/Auth', {
            body: {
                'phone': phone
            }
        });
    }
    
    this.hash = function(login, password) {
        var h = base64.encode(login + ":" + password);
        $.client.hash = h;
    }
    
    this.getClient = function() {
        var result = this.request('get', '/Clients', {});
        return result.isOk && result.data.length > 0 ? result.data[0] : false; 
    }
    
    this.getLastOrder = function() {
        var result = this.request('get', '/Orders/LastOrder', {
            headers: {
                'client-id': $.client.id
            }
        });

        return result.isOk && result.data.length > 0 ? result.data[0] : false;
    }
    
    this.getOrders = function(ordersCount) {
        ordersCount = ordersCount || 1;
        
        var result = this.request('get', '/Orders', {
            headers: {
                'client-id': $.client.id
            }
        });
        
        var toReturn = [];
        if (result.isOk && result.data.length > 0){
            result.data.forEach(function(order) {
                order.items.forEach(function(item) {
                    if (item.productGroupCode !== "GR_NOT_ACTIVE") toReturn.push(item.id);
                })
            });
            
            toReturn = _.uniq(toReturn);
            toReturn = _.first(toReturn, ordersCount);
        }
        return toReturn.length > 0 ? toReturn : false;
    }
    
    this.getDicounts = function() {
        var result = this.request('get', '/Actions', {
            headers: {
                'client-id': $.client.id
            }
        });
        return result.isOk? result.data : false;
    }
    
    this.getDiscount = function(id) {
        var result = this.request('get', '/Actions/' + id, {
            headers: {
                'client-id': $.client.id
            }
        });
        return result.isOk? result.data : false;
    }
    
    this.getCatalog = function() {
        var result = this.request('get', '/Catalog', {
            headers: {
                'client-id': $.client.id
            }
        });
        return result.isOk? result.data : false;
    }
    
    this.log = function(response, httpMethod, apiMethod, paramsObj) {
        var toLog = httpMethod + ' | ' + apiMethod + ' | ' + toPrettyString(paramsObj) + ' | ' + response.status + ' | ';
        if (response.isOk) {
            toLog += toPrettyString(response.data);
        } else {
            toLog += toPrettyString(response.error);
        }
        if (toLog.length < 50000) $analytics.setComment(toLog);
    }
}