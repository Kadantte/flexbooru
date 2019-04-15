package onlymash.flexbooru.repository.pool

import onlymash.flexbooru.api.SankakuApi
import onlymash.flexbooru.api.url.SankakuUrlHelper
import onlymash.flexbooru.entity.pool.PoolSankaku
import onlymash.flexbooru.entity.Search
import onlymash.flexbooru.repository.BasePageKeyedDataSource
import retrofit2.Call
import retrofit2.Response
import java.util.concurrent.Executor

// sankaku pools data source
class PoolSankakuDataSource(private val sankakuApi: SankakuApi,
                            private val search: Search,
                            retryExecutor: Executor) : BasePageKeyedDataSource<Int, PoolSankaku>(retryExecutor) {

    override fun loadInitialRequest(params: LoadInitialParams<Int>, callback: LoadInitialCallback<Int, PoolSankaku>) {
        val request = sankakuApi.getPools(SankakuUrlHelper.getPoolUrl(search = search, page = 1))
        val scheme = search.scheme
        val host = search.host
        val keyword = search.keyword
        val response =  request.execute()
        val data = response.body() ?: mutableListOf()
        data.forEach {
            it.scheme = scheme
            it.host = host
            it.keyword = keyword
        }
        if (data.size < search.limit) {
            callback.onResult(data, null, null)
            onEnd()
        } else {
            callback.onResult(data, null, 2)
        }
    }

    override fun loadAfterRequest(params: LoadParams<Int>, callback: LoadCallback<Int, PoolSankaku>) {
        val page = params.key
        sankakuApi.getPools(SankakuUrlHelper.getPoolUrl(search = search, page = page))
            .enqueue(object : retrofit2.Callback<MutableList<PoolSankaku>> {
                override fun onFailure(call: Call<MutableList<PoolSankaku>>, t: Throwable) {
                    loadAfterOnFailed(t.message ?: "unknown err", params, callback)
                }
                override fun onResponse(call: Call<MutableList<PoolSankaku>>, response: Response<MutableList<PoolSankaku>>) {
                    if (response.isSuccessful) {
                        val data = response.body() ?: mutableListOf()
                        val scheme = search.scheme
                        val host = search.host
                        val keyword = search.keyword
                        data.forEach {
                            it.scheme = scheme
                            it.host = host
                            it.keyword = keyword
                        }
                        loadAfterOnSuccess()
                        if (data.size < search.limit) {
                            callback.onResult(data, null)
                            onEnd()
                        } else {
                            callback.onResult(data, page + 1)
                        }
                    } else {
                        loadAfterOnFailed("error code: ${response.code()}", params, callback)
                    }
                }
            })
    }
}