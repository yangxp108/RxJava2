package com.malin.rxjava2;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.BufferedReader;
import java.io.FileReader;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

//http://www.jianshu.com/p/9b1304435564
public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private Subscription mSubscription;//订阅

    //emitter 发射器

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSubscription.request(96);
            }
        });

        test5();
    }

    private void test() {

        /**
         * 首先是创建Flowable的时候增加了一个参数, 这个参数是用来选择背压,
         * 也就是出现上下游流速不均衡的时候应该怎么处理的办法,
         * 这里我们直接用BackpressureStrategy.ERROR这种方式,
         * 这种方式会在出现上下游流速不均衡的时候直接抛出一个异常,
         * 这个异常就是著名的MissingBackpressureException.
         */
        Flowable<Integer> upstream = Flowable.create(new FlowableOnSubscribe<Integer>() {
            @Override
            public void subscribe(FlowableEmitter<Integer> emitter) throws Exception {
                Log.d(TAG, "emit 1");
                emitter.onNext(1);
                Log.d(TAG, "emit 2");
                emitter.onNext(2);
                Log.d(TAG, "emit 3");
                emitter.onNext(3);
                Log.d(TAG, "emit complete");
                emitter.onComplete();
            }
        }, BackpressureStrategy.ERROR); //增加了一个参数

        Subscriber<Integer> downstream = new Subscriber<Integer>() {

            @Override
            public void onSubscribe(Subscription subscription) {
                Log.d(TAG, "onSubscribe");
                mSubscription = subscription;
                /**
                 * request当做是一种能力, 当成下游处理事件的能力,
                 * 下游能处理几个就告诉上游我要几个,
                 * 这样只要上游根据下游的处理能力来决定发送多少事件,
                 * 就不会造成一窝蜂的发出一堆事件来, 从而导致OOM.
                 * 这也就完美的解决之前我们所学到的两种方式的缺陷,
                 * 过滤事件会导致事件丢失, 减速又可能导致性能损失.
                 * 而这种方式既解决了事件丢失的问题, 又解决了速度的问题, 完美 !
                 */
                //subscription.request(Long.MAX_VALUE);  //注意这句代码
            }

            @Override
            public void onNext(Integer integer) {
                Log.d(TAG, "onNext: " + integer);

            }

            @Override
            public void onError(Throwable t) {
                Log.w(TAG, "onError: ", t);
            }

            @Override
            public void onComplete() {
                Log.d(TAG, "onComplete");
            }
        };

        upstream.subscribe(downstream);

    }

    private void test2() {

        /**
         * Flowable里默认有一个大小为128的水缸, 当上下游工作在不同的线程中时,
         * 上游就会先把事件发送到这个水缸中,
         * 因此, 下游虽然没有调用request, 但是上游在水缸中保存着这些事件,
         * 只有当下游调用request时, 才从水缸里取出事件发给下游.
         */
        Flowable.create(new FlowableOnSubscribe<Integer>() {
            @Override
            public void subscribe(FlowableEmitter<Integer> emitter) throws Exception {
                Log.d(TAG, "emit 1");
                emitter.onNext(1);
                Log.d(TAG, "emit 2");
                emitter.onNext(2);
                Log.d(TAG, "emit 3");
                emitter.onNext(3);
                Log.d(TAG, "emit complete");
                emitter.onComplete();
            }
        }, BackpressureStrategy.ERROR)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Integer>() {

                    @Override
                    public void onSubscribe(Subscription s) {
                        mSubscription = s;
                        Log.d(TAG, "onSubscribe");
                    }

                    @Override
                    public void onNext(Integer integer) {
                        Log.d(TAG, "onNext: " + integer);
                    }

                    @Override
                    public void onError(Throwable t) {
                        Log.w(TAG, "onError: ", t);
                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG, "onComplete");
                    }
                });
    }

    private void test3() {
        Flowable.create(new FlowableOnSubscribe<String>() {
            @Override
            public void subscribe(FlowableEmitter<String> emitter) throws Exception {
                Log.d(TAG, "before emit, requested = " + emitter.requested());

                Log.d(TAG, "emit 1");
                emitter.onNext(String.valueOf(1));
                Log.d(TAG, "after emit 1, requested = " + emitter.requested());

                Log.d(TAG, "emit 2");
                emitter.onNext(String.valueOf(2));
                Log.d(TAG, "after emit 2, requested = " + emitter.requested());

                Log.d(TAG, "emit 3");
                emitter.onNext(String.valueOf(3));
                Log.d(TAG, "after emit 3, requested = " + emitter.requested());

                Log.d(TAG, "emit complete");
                emitter.onComplete();

                Log.d(TAG, "after emit complete, requested = " + emitter.requested());
            }
        }, BackpressureStrategy.ERROR)
                .subscribe(new Subscriber<String>() {
                    @Override
                    public void onSubscribe(Subscription subscription) {
                        Log.d(TAG, "onSubscribe()");
                        subscription.request(2);
                    }

                    @Override
                    public void onNext(String s) {
                        Log.d(TAG, "onNext:" + s.toString());
                    }

                    @Override
                    public void onError(Throwable throwable) {

                        Log.w(TAG, "onError", throwable);
                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG, "onComplete()");
                    }
                });

    }

    private void test4() {
        Flowable.create(new FlowableOnSubscribe<String>() {
            @Override
            public void subscribe(FlowableEmitter<String> flowableEmitter) throws Exception {
                Log.d(TAG, "flowableEmitter.requested():" + flowableEmitter.requested());
//                for (int i = 0; i < 129; i++) {
//                    flowableEmitter.onNext(String.valueOf(i));
//                    Log.d(TAG, "Emit:" + i);
//                }
            }
        }, BackpressureStrategy.ERROR)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<String>() {
                    @Override
                    public void onSubscribe(Subscription subscription) {
                        Log.d(TAG, "onSubscribe()");
                        subscription.request(1000);
                    }

                    @Override
                    public void onNext(String s) {
                        Log.d(TAG, "onNext:" + s.toString());
                    }

                    @Override
                    public void onError(Throwable throwable) {

                        Log.w(TAG, "onError", throwable);
                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG, "onComplete()");
                    }
                });
    }


    public void test5() {
        Flowable
                .create(new FlowableOnSubscribe<Integer>() {
                    @Override
                    public void subscribe(FlowableEmitter<Integer> emitter) throws Exception {
                        Log.d(TAG, "First requested = " + emitter.requested());
                        boolean flag;
                        for (int i = 0; ; i++) {
                            flag = false;
                            while (emitter.requested() == 0) {
                                if (!flag) {
                                    Log.d(TAG, "Oh no! I can't emit value!");
                                    flag = true;
                                }
                            }
                            emitter.onNext(i);
                            Log.d(TAG, "emit " + i + " , requested = " + emitter.requested());
                        }
                    }
                }, BackpressureStrategy.ERROR)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Integer>() {

                    @Override
                    public void onSubscribe(Subscription subscription) {
                        Log.d(TAG, "onSubscribe");
                        mSubscription = subscription;
                    }

                    @Override
                    public void onNext(Integer integer) {
                        Log.d(TAG, "onNext: " + integer);
                    }

                    @Override
                    public void onError(Throwable t) {
                        Log.w(TAG, "onError: ", t);
                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG, "onComplete");
                    }
                });
    }

    public void practice1() {
        Flowable
                .create(new FlowableOnSubscribe<String>() {
                    @Override
                    public void subscribe(FlowableEmitter<String> emitter) throws Exception {
                        try {
                            FileReader reader = new FileReader("test.txt");
                            BufferedReader br = new BufferedReader(reader);

                            String str;

                            while ((str = br.readLine()) != null && !emitter.isCancelled()) {
                                while (emitter.requested() == 0) {
                                    if (emitter.isCancelled()) {
                                        break;
                                    }
                                }
                                emitter.onNext(str);
                            }

                            br.close();
                            reader.close();

                            emitter.onComplete();
                        } catch (Exception e) {
                            emitter.onError(e);
                        }
                    }
                }, BackpressureStrategy.ERROR)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.newThread())
                .subscribe(new Subscriber<String>() {

                    @Override
                    public void onSubscribe(Subscription s) {
                        mSubscription = s;
                        s.request(1);
                    }

                    @Override
                    public void onNext(String string) {
                        System.out.println(string);
                        try {
                            Thread.sleep(2000);
                            mSubscription.request(1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        System.out.println(t);
                    }

                    @Override
                    public void onComplete() {
                    }
                });
    }
}
