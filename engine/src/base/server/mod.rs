use crate::db::query::{Query, QuerySelect};
use crate::Database;
use actix_web::rt::task::spawn_blocking;
use actix_web::{get, post, web, App, HttpResponse, HttpServer, Responder};
use std::sync::Arc;

#[actix_web::main]
pub async fn main(database: Arc<Database>, port: u16) -> std::io::Result<()> {
    let database = database.clone();
    println!("Database loaded. Starting HTTP server..");
    HttpServer::new(move || {
        App::new()
            .app_data(web::Data::new(database.clone()))
            .service(hello)
            .service(query)
    })
    .bind(("127.0.0.1", port))?
    .run()
    .await
}

#[get("/")]
async fn hello() -> impl Responder {
    HttpResponse::Ok().body("Hello, this is the engine of LAPIS v2!")
}

#[post("/query")]
async fn query(bytes: web::Bytes, database: web::Data<Arc<Database>>) -> impl Responder {
    let req_body = std::str::from_utf8(&bytes).ok();
    if let Some(req_body) = req_body {
        let req_body = req_body.to_string();
        let result = spawn_blocking(move || {
            let query = Query::from_json(req_body.as_str())?;
            let filtered = query.filter.evaluate(&database);
            match query.select {
                QuerySelect::Aggregated(query) => Some(query.evaluate(&filtered, &database).to_json(&database)),
                QuerySelect::Details(_) => {
                    todo!()
                }
                QuerySelect::NucSequences(_) => {
                    todo!()
                }
                QuerySelect::NucMutations(query) => Some(query.evaluate(&filtered, &database).to_json()),
            }
        })
        .await;
        if let Ok(Some(result)) = result {
            return HttpResponse::Ok().body(result);
        }
    }
    return HttpResponse::BadRequest().body("Malformed or not allowed query");
}
