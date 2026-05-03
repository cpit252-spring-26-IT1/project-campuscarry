class Subject {
  constructor() {
    this.observers = new Set();
  }

  subscribe(observer) {
    if (!observer || typeof observer.update !== "function") {
      throw new Error("Observer must implement update(event).");
    }

    this.observers.add(observer);

    return () => {
      this.unsubscribe(observer);
    };
  }

  unsubscribe(observer) {
    this.observers.delete(observer);
  }

  notify(event) {
    this.observers.forEach((observer) => {
      try {
        observer.update(event);
      } catch (error) {
        console.error("Observer notification failed:", error);
      }
    });
  }
}

export default Subject;
