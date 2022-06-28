use crossbeam::deque::{Injector, Steal};
use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::Arc;
use std::thread;
use std::time::Duration;

pub struct ExecutorService {
    tasks: Arc<Injector<Box<dyn FnOnce(usize) + Send>>>,
    closed: Arc<AtomicBool>,
    worker_is_working: Vec<Arc<AtomicBool>>,
}

impl ExecutorService {
    pub fn new(size: usize) -> Self {
        if size <= 0 {
            panic!("The size of the ExecutorService must be larger than 0.");
        }
        // Prepare data structures
        let tasks = Arc::new(Injector::<Box<dyn FnOnce(usize) + Send>>::new());
        let mut worker_is_working = vec![];
        let closed = Arc::new(AtomicBool::new(false));
        // Start threads
        for i in 0..size {
            let is_working = Arc::new(AtomicBool::new(false));
            worker_is_working.push(is_working.clone());
            let tasks = tasks.clone();
            let closed = closed.clone();
            thread::spawn(move || {
                let id = i;
                let i_am_working = is_working;
                loop {
                    if closed.load(Ordering::Relaxed) && tasks.is_empty() {
                        break;
                    }
                    i_am_working.store(true, Ordering::Relaxed);
                    match tasks.steal() {
                        Steal::Empty => {
                            i_am_working.store(false, Ordering::Relaxed);
                            thread::sleep(Duration::from_secs(1))
                        }
                        Steal::Success(task) => {
                            task(id);
                        }
                        Steal::Retry => {}
                    }
                    i_am_working.store(false, Ordering::Relaxed);
                }
            });
        }
        // Create final object
        ExecutorService {
            tasks,
            closed,
            worker_is_working,
        }
    }

    pub fn submit_task<F: 'static + FnOnce(usize) + Send>(&mut self, task: F) {
        if self.closed.load(Ordering::Relaxed) {
            panic!("It is not allowed to submit tasks after the ExecutorService is closed.");
        }
        self.tasks.push(Box::new(task));
    }

    /// Waits until all existing tasks are finished
    pub fn join(&self) {
        loop {
            if self.tasks.is_empty() {
                let mut no_one_is_working = true;
                for worker in &self.worker_is_working {
                    if worker.load(Ordering::Relaxed) {
                        no_one_is_working = false;
                        break;
                    }
                }
                if no_one_is_working {
                    break;
                }
            }
            thread::sleep(Duration::from_secs(1));
        }
    }

    /// Stops the threads after processing all tasks
    pub fn close(&self) {
        self.closed.store(true, Ordering::Relaxed);
    }
}

impl Drop for ExecutorService {
    fn drop(&mut self) {
        self.close();
    }
}
