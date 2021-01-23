# SpringWebFlux-V2

## WebFlux와 R2DBC와 Processor(Emiiter)결합해보기

- repository를 이용해서 transaction을 발동시킬 때는 꼭 response응답을 위해 return을 해줘야 한다.
- return 하지 않으면 트랜잭션 발동이 안됨.

![img](https://github.com/codingspecialist/SpringWebFlux-V3/blob/master/preview/1.png)
![img](https://github.com/codingspecialist/SpringWebFlux-V3/blob/master/preview/2.png)
![img](https://github.com/codingspecialist/SpringWebFlux-V3/blob/master/preview/3.png)
![img](https://github.com/codingspecialist/SpringWebFlux-V3/blob/master/preview/4.png)