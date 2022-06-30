use crate::{operators, Database};
use actix_web::rt::task::spawn_blocking;
use actix_web::{get, post, web, App, HttpResponse, HttpServer, Responder};
use std::sync::Arc;

#[actix_web::main]
pub async fn main(database: Arc<Database>) -> std::io::Result<()> {
    let database = database.clone();
    HttpServer::new(move || {
        App::new()
            .app_data(web::Data::new(database.clone()))
            .service(hello)
            .service(query)
    })
    .bind(("127.0.0.1", 8080))?
    .run()
    .await
}

#[get("/")]
async fn hello() -> impl Responder {
    HttpResponse::Ok().body("Hello, this is the engine of LAPIS v2!")
}

#[post("/query")]
async fn query(bytes: web::Bytes, data: web::Data<Arc<Database>>) -> impl Responder {
    let query_str = std::str::from_utf8(&bytes).ok();
    if let Some(query_str) = query_str {
        let query_str = query_str.to_string();
        let result = spawn_blocking(move || {
            let query = operators::from_json(query_str.as_str());
            if let Some(query) = query {
                Some(query.evaluate(&data))
            } else {
                None
            }
        })
        .await;
        if let Ok(Some(result)) = result {
            return HttpResponse::Ok().body(format!("{:?}", result));
        }
    }
    return HttpResponse::BadRequest().body("Malformed or not allowed query");
}
