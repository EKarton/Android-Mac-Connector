var mqtt = require('mqtt')

var client  = mqtt.connect('ws://192.168.0.102:8888', {
  clientId: '',
  username: '',
  password: ''
})
 
client.on('connect', function () {
  const deviceId = 'WRF2KV4cefu0HuH8LDks'
  const publishOptions = {
    qos: 2,
    retain: true
  }
  console.log("Publishing message")

  // const sendSmsMessage = {
  //   "phone_number": "647-607-6358 647-666-9090 324-234-2344",
  //   "message": "Testing message",
  //   "message_id": "3",
  // }
  // client.publish(`${deviceId}/send-sms-request`, JSON.stringify(sendSmsMessage), publishOptions, (error, packet) => {
  //   if (error) {
  //     console.error(error)
  //   }
  //   client.end()
  // })

  // const queryThreads = {
  //   "limit": 10,
  //   "start": 0,
  // }
  // client.publish(`${deviceId}/sms/threads/query-requests`, JSON.stringify(queryThreads), publishOptions, (error, packet) => {
  //   if (error) {
  //     console.error(error)
  //   }
  //   client.end()
  // })

  const queryMessages = {
    "limit": 10,
    "start": 0,
    "thread_id": 12,
  }
  client.publish(`${deviceId}/sms/messages/query-requests`, JSON.stringify(queryMessages), publishOptions, (error, packet) => {
    if (error) {
      console.error(error)
    }
    client.end()
  })
})

client.on('error', (error) => {
  console.error(error)
});
