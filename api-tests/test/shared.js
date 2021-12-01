const Ajv = require('ajv/dist/jtd')
const validators = require('./validators')
const {ajv} = require("./validators");
const supertest = require("supertest");


const apiUrl = process.env.API_URL;
const server = supertest.agent(apiUrl);
const openness = process.env.OPENNESS

console.log('Testing ' + apiUrl + ' with openness level ' + openness);


const hasRightGeneralSchema = (res) => {
  const body = res.body;
  if (!validators.generalSchema(body)) {
    throw new Error('The response body has the wrong general schema.');
  }
};

const hasNoErrorEntries = (res) => {
  const body = res.body;
  if (body.errors.length > 0) {
    throw new Error('Errors are being reported');
  }
};

const hasNoPayload = (res) => {
  const body = res.body;
  if (body.data !== null) {
    throw new Error('Found data');
  }
};

const isOkay = (req) => {
  return req
    .expect(200)
    .expect('Content-Type', /json/)
    .expect(hasRightGeneralSchema)
    .expect(hasNoErrorEntries);
};

const isNotOkay = (req) => {
  return req
    .expect((res) => res.status !== 200)
    .expect('Content-Type', /json/)
    .expect(hasRightGeneralSchema)
    .expect(hasNoPayload);
};

const checkPayloadFromSchema = (schema) => {
  return (res) => {
    const validate = ajv.compile(schema);
    if (!validate(res.body.data)) {
      throw new Error('The data has the wrong schema: \n' + JSON.stringify(validate.errors, null, 2));
    }
  };
}


module.exports = {
  // Functions
  isOkay,
  isNotOkay,
  checkPayloadFromSchema,

  // Shared variables
  apiUrl,
  server,
  openness,
};
