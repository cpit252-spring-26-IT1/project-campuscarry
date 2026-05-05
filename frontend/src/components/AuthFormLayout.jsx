import { Card, CardBody } from "@heroui/react";

const AuthFormLayout = ({ title, onSubmit, children, footer = null }) => {
  return (
    <section className="bg-transparent">
      <div className="container m-auto max-w-2xl py-24">
        <Card className="m-4 border border-[#3c4748]/55 bg-[#f4fbfb]/90 shadow-xl backdrop-blur-sm dark:border-[#3c4748]/70 dark:bg-[#273b40]/55 md:m-0">
          <CardBody className="px-6 py-8">
            <form onSubmit={onSubmit}>
              <h2 className="mb-6 text-center font-display text-3xl font-semibold tracking-tight text-[#1d1d1d] dark:text-[#cae9ea]">
                {title}
              </h2>
              {children}
              {footer}
            </form>
          </CardBody>
        </Card>
      </div>
    </section>
  );
};

export default AuthFormLayout;
