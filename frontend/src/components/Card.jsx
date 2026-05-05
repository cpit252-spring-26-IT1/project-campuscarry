import { Card as HeroCard, CardBody } from "@heroui/react";

const Card = ({ children, bg = "bg-[#eaf4f4]/85 dark:bg-[#273b40]/65", className = "" }) => {
  return (
    <HeroCard
      className={`${bg} ${className} border border-[#3c4748]/40 text-[#1d1d1d] shadow-md dark:border-[#3c4748]/90 dark:text-[#cae9ea]`}
      radius="lg"
      shadow="none"
    >
      <CardBody className="p-6">{children}</CardBody>
    </HeroCard>
  );
};

export default Card;
